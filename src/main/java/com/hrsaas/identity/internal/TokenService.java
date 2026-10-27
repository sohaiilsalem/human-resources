package com.hrsaas.identity.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.hrsaas.identity.Permission;
import com.hrsaas.platform.TenantInfo;

/**
 * Issues access tokens (JWT, short-lived) and refresh tokens. A refresh token has the form
 * {@code <tenantId>.<random>} so the tenant can be bound before the token row is looked up under row-level security.
 */
@Component
class TokenService {

	static final String TENANT_CLAIM = "tid";

	static final String ROLES_CLAIM = "roles";

	static final String PERMISSIONS_CLAIM = "permissions";

	private static final SecureRandom RANDOM = new SecureRandom();

	private final JwtEncoder encoder;

	private final JwtProperties properties;

	private final RefreshTokenRepository refreshTokens;

	private final Clock clock;

	TokenService(JwtEncoder encoder, JwtProperties properties, RefreshTokenRepository refreshTokens, Clock clock) {
		this.encoder = encoder;
		this.properties = properties;
		this.refreshTokens = refreshTokens;
		this.clock = clock;
	}

	/** Must run inside a transaction bound to {@code tenant}. */
	IssuedTokens issue(User user, TenantInfo tenant) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(user.getId().toString())
			.issuedAt(now)
			.expiresAt(now.plus(properties.accessTokenTtl()))
			.claim(TENANT_CLAIM, tenant.id().toString())
			.claim("tenant", tenant.slug())
			.claim(ROLES_CLAIM, user.getRoleCodes())
			.claim(PERMISSIONS_CLAIM, user.getPermissions().stream().map(Permission::authority).sorted().toList())
			.build();
		String accessToken = encoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		byte[] secret = new byte[32];
		RANDOM.nextBytes(secret);
		String refreshToken = tenant.id() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
		RefreshToken stored = refreshTokens
			.save(new RefreshToken(user.getId(), hash(refreshToken), now.plus(properties.refreshTokenTtl())));

		return new IssuedTokens(new TokenResponse(accessToken, "Bearer", properties.accessTokenTtl().toSeconds(),
				refreshToken, properties.refreshTokenTtl().toSeconds()), stored.getId());
	}

	static Optional<Long> tenantOf(String refreshToken) {
		int dot = refreshToken.indexOf('.');
		if (dot <= 0) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.valueOf(refreshToken.substring(0, dot)));
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	record IssuedTokens(TokenResponse response, Long refreshTokenId) {
	}

}
