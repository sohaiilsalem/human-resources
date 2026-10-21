package com.hrsaas.identity.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrsaas.identity.Role;
import com.hrsaas.platform.RegisterTenant;
import com.hrsaas.platform.TenantDirectory;
import com.hrsaas.platform.TenantInfo;
import com.hrsaas.shared.persistence.Ids;
import com.hrsaas.shared.tenant.TenantContext;
import com.hrsaas.shared.web.ApiException;

/**
 * Sign-up, login and refresh-token rotation. Each flow resolves the tenant first and then opens its transaction
 * inside {@link TenantContext#callAs}, so the Hibernate session and row-level security see that tenant.
 */
@Service
class AuthService {

	private static final String INVALID_CREDENTIALS = "auth.invalid-credentials";

	private static final String INVALID_REFRESH_TOKEN = "auth.invalid-refresh-token";

	private final TenantDirectory tenants;

	private final UserRepository users;

	private final RefreshTokenRepository refreshTokens;

	private final TokenService tokens;

	private final PasswordEncoder passwordEncoder;

	private final TransactionTemplate tx;

	private final Clock clock;

	/** Compared against when the user does not exist, so response time does not reveal valid emails. */
	private final String dummyHash;

	AuthService(TenantDirectory tenants, UserRepository users, RefreshTokenRepository refreshTokens,
			TokenService tokens, PasswordEncoder passwordEncoder, TransactionTemplate tx, Clock clock) {
		this.tenants = tenants;
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.tokens = tokens;
		this.passwordEncoder = passwordEncoder;
		this.tx = tx;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode("timing-equalisation-only");
	}

	TokenResponse signUp(AuthRequests.SignUp request) {
		UUID tenantId = Ids.newId();
		String locale = request.locale() != null ? request.locale() : "en";
		return TenantContext.callAs(tenantId, () -> tx.execute(status -> {
			TenantInfo tenant = tenants.register(new RegisterTenant(tenantId, normalize(request.tenant()),
					request.companyName().strip(), locale));
			User owner = users.save(new User(normalize(request.email()), request.fullName().strip(),
					passwordEncoder.encode(request.password()), locale, Set.of(Role.OWNER), null));
			return tokens.issue(owner, tenant).response();
		}));
	}

	TokenResponse login(AuthRequests.Login request) {
		TenantInfo tenant = tenants.findBySlug(normalize(request.tenant()))
			.filter(TenantInfo::isActive)
			.orElseThrow(() -> ApiException.unauthorized(INVALID_CREDENTIALS));
		return TenantContext.callAs(tenant.id(), () -> tx.execute(status -> {
			Optional<User> user = users.findByEmail(normalize(request.email()));
			String hash = user.map(User::getPasswordHash).orElse(dummyHash);
			boolean matches = passwordEncoder.matches(request.password(), hash);
			if (user.isEmpty() || !matches || !user.get().isActive()) {
				throw ApiException.unauthorized(INVALID_CREDENTIALS);
			}
			user.get().recordLogin(clock.instant());
			return tokens.issue(user.get(), tenant).response();
		}));
	}

	/**
	 * Rotates the refresh token. Presenting an already-rotated token is treated as theft: every refresh token of that
	 * user is revoked (committed before the error is returned).
	 */
	TokenResponse refresh(String refreshToken) {
		TenantInfo tenant = TokenService.tenantOf(refreshToken)
			.flatMap(tenants::findById)
			.filter(TenantInfo::isActive)
			.orElseThrow(() -> ApiException.unauthorized(INVALID_REFRESH_TOKEN));
		Optional<TokenResponse> rotated = TenantContext.callAs(tenant.id(), () -> tx.execute(status -> {
			Instant now = clock.instant();
			RefreshToken current = refreshTokens.findByTokenHash(TokenService.hash(refreshToken)).orElse(null);
			if (current == null || current.isExpired(now)) {
				return Optional.<TokenResponse>empty();
			}
			if (current.isRevoked()) {
				refreshTokens.revokeAllForUser(current.getUserId(), now);
				return Optional.<TokenResponse>empty();
			}
			Optional<User> user = users.findById(current.getUserId()).filter(User::isActive);
			if (user.isEmpty()) {
				current.revoke(now, null);
				return Optional.<TokenResponse>empty();
			}
			TokenService.IssuedTokens issued = tokens.issue(user.get(), tenant);
			current.revoke(now, issued.refreshTokenId());
			return Optional.of(issued.response());
		}));
		return rotated.orElseThrow(() -> ApiException.unauthorized(INVALID_REFRESH_TOKEN));
	}

	void logout(String refreshToken) {
		TokenService.tenantOf(refreshToken).ifPresent(tenantId -> TenantContext.runAs(tenantId, () -> tx
			.executeWithoutResult(status -> refreshTokens.findByTokenHash(TokenService.hash(refreshToken))
				.filter(token -> !token.isRevoked())
				.ifPresent(token -> token.revoke(clock.instant(), null)))));
	}

	private static String normalize(String value) {
		return value.strip().toLowerCase(Locale.ROOT);
	}

}
