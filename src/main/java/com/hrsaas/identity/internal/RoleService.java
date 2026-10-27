package com.hrsaas.identity.internal;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.identity.Permission;
import com.hrsaas.shared.web.ApiException;

/**
 * Tenant roles. Nobody can hand out more than they hold: creating, changing or granting a role requires the caller
 * to have every permission in it, so {@code user:manage} alone cannot be escalated to billing or payroll.
 */
@Service
@Transactional(readOnly = true)
class RoleService {

	private final RoleRepository roles;

	RoleService(RoleRepository roles) {
		this.roles = roles;
	}

	/** Must run inside the sign-up transaction of the new tenant; returns its owner role. */
	@Transactional
	Role seedDefaults() {
		return roles.saveAll(DefaultRoles.create()).stream().filter(Role::isOwner).findFirst().orElseThrow();
	}

	List<Role> list() {
		return roles.findAllByOrderById();
	}

	List<Role> findByCodes(Collection<String> codes) {
		return codes.isEmpty() ? List.of() : roles.findByCodeIn(Set.copyOf(codes));
	}

	@Transactional
	Role create(CreateRole request) {
		if (roles.existsByCode(request.code())) {
			throw ApiException.conflict("role.code-taken", request.code());
		}
		Set<Permission> permissions = parse(request.permissions());
		requireHeldByCaller(permissions);
		return roles.save(new Role(request.code(), request.nameEn(), request.nameAr(), permissions));
	}

	@Transactional
	Role update(String code, UpdateRole request) {
		Role role = get(code);
		if (role.isOwner()) {
			throw ApiException.forbidden("role.owner-immutable");
		}
		Set<Permission> permissions = parse(request.permissions());
		// both what is taken away and what is added must be within the caller's own permissions
		requireHeldByCaller(role.getPermissions());
		requireHeldByCaller(permissions);
		role.apply(request.nameEn(), request.nameAr(), permissions);
		return role;
	}

	@Transactional
	void delete(String code) {
		Role role = get(code);
		if (role.isOwner()) {
			throw ApiException.forbidden("role.owner-immutable");
		}
		long users = roles.countUsers(role);
		if (users > 0) {
			throw ApiException.conflict("role.in-use", code, users);
		}
		requireHeldByCaller(role.getPermissions());
		roles.delete(role);
	}

	/**
	 * Resolves the codes of roles to grant to a user.
	 * @throws ApiException if a code is unknown, is {@code OWNER}, or grants more than the caller holds
	 */
	Set<Role> resolveGrantable(Collection<String> codes) {
		Set<String> wanted = Set.copyOf(codes);
		List<Role> found = roles.findByCodeIn(wanted);
		if (found.size() != wanted.size()) {
			Set<String> known = found.stream().map(Role::getCode).collect(Collectors.toSet());
			String missing = wanted.stream().filter(code -> !known.contains(code)).sorted().findFirst().orElseThrow();
			throw ApiException.unprocessable("role.not-found", missing);
		}
		if (found.stream().anyMatch(Role::isOwner)) {
			throw ApiException.forbidden("user.cannot-grant-owner");
		}
		found.forEach(role -> requireHeldByCaller(role.getPermissions()));
		return Set.copyOf(found);
	}

	private Role get(String code) {
		return roles.findByCode(code).orElseThrow(() -> ApiException.notFound("role.not-found", code));
	}

	private static Set<Permission> parse(Collection<String> authorities) {
		Set<Permission> permissions = EnumSet.noneOf(Permission.class);
		for (String authority : authorities) {
			permissions.add(Permission.fromAuthority(authority)
				.orElseThrow(() -> ApiException.unprocessable("role.unknown-permission", authority)));
		}
		return permissions;
	}

	private static void requireHeldByCaller(Set<Permission> permissions) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Set<String> held = authentication == null ? Set.of()
				: authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
		if (!permissions.stream().map(Permission::authority).allMatch(held::contains)) {
			throw ApiException.forbidden("role.exceeds-own-permissions");
		}
	}

	record CreateRole(@NotBlank @Pattern(regexp = "[A-Z0-9_]{2,30}", message = "{validation.code}") String code,
			@NotBlank @Size(max = 100) String nameEn, @NotBlank @Size(max = 100) String nameAr,
			@NotNull Set<String> permissions) {
	}

	record UpdateRole(@NotBlank @Size(max = 100) String nameEn, @NotBlank @Size(max = 100) String nameAr,
			@NotNull Set<String> permissions) {
	}

}
