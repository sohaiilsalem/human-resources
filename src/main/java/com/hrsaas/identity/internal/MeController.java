package com.hrsaas.identity.internal;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.identity.Permission;
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
		User user = users.findById(Long.valueOf(jwt.getSubject()))
			.orElseThrow(() -> ApiException.notFound("user.not-found"));
		TenantInfo tenant = tenants.findById(TenantContext.require()).orElseThrow();
		List<String> permissions = user.getPermissions().stream().map(Permission::authority).sorted().toList();
		return new MeResponse(user.getId(), user.getEmail(), user.getFullName(), user.getLocale(), user.getEmployeeId(),
				user.getRoleCodes(), permissions,
				new MeResponse.Tenant(tenant.id(), tenant.slug(), tenant.name()));
	}

	record MeResponse(Long id, String email, String fullName, String locale, Long employeeId, List<String> roles,
			List<String> permissions, Tenant tenant) {

		record Tenant(Long id, String slug, String name) {
		}

	}

}
