package com.hrsaas.identity.internal;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.identity.Permission;
import com.hrsaas.identity.Role;
import com.hrsaas.platform.TenantDirectory;
import com.hrsaas.platform.TenantInfo;
import com.hrsaas.shared.tenant.TenantContext;
import com.hrsaas.shared.web.ApiException;

@RestController
class MeController {

	private final UserRepository users;

	private final TenantDirectory tenants;

	MeController(UserRepository users, TenantDirectory tenants) {
		this.users = users;
		this.tenants = tenants;
	}

	@GetMapping("/api/v1/me")
	@Transactional(readOnly = true)
	MeResponse me(@AuthenticationPrincipal Jwt jwt) {
		User user = users.findById(UUID.fromString(jwt.getSubject()))
			.orElseThrow(() -> ApiException.notFound("user.not-found"));
		TenantInfo tenant = tenants.findById(TenantContext.require()).orElseThrow();
		List<String> permissions = user.getRoles()
			.stream()
			.flatMap(role -> role.permissions().stream())
			.map(Permission::authority)
			.distinct()
			.sorted()
			.toList();
		List<Role> roles = user.getRoles().stream().sorted(Comparator.naturalOrder()).toList();
		return new MeResponse(user.getId(), user.getEmail(), user.getFullName(), user.getLocale(), user.getEmployeeId(),
				roles, permissions,
				new MeResponse.Tenant(tenant.id(), tenant.slug(), tenant.name()));
	}

	record MeResponse(UUID id, String email, String fullName, String locale, UUID employeeId, List<Role> roles,
			List<String> permissions, Tenant tenant) {

		record Tenant(UUID id, String slug, String name) {
		}

	}

}
