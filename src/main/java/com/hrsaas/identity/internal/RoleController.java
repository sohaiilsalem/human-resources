package com.hrsaas.identity.internal;

import java.util.Arrays;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.identity.Permission;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasAuthority('user:manage')")
class RoleController {

	private final RoleService roles;

	RoleController(RoleService roles) {
		this.roles = roles;
	}

	@GetMapping
	List<RoleResponse> list() {
		return roles.list().stream().map(RoleResponse::of).toList();
	}

	/** Every permission a role can hold. */
	@GetMapping("/permissions")
	List<String> permissions() {
		return Arrays.stream(Permission.values()).map(Permission::authority).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	RoleResponse create(@Valid @RequestBody RoleService.CreateRole request) {
		return RoleResponse.of(roles.create(request));
	}

	@PutMapping("/{code}")
	RoleResponse update(@PathVariable String code, @Valid @RequestBody RoleService.UpdateRole request) {
		return RoleResponse.of(roles.update(code, request));
	}

	@DeleteMapping("/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable String code) {
		roles.delete(code);
	}

	record RoleResponse(Long id, String code, String nameEn, String nameAr, List<String> permissions,
			boolean owner) {

		static RoleResponse of(Role role) {
			return new RoleResponse(role.getId(), role.getCode(), role.getNameEn(), role.getNameAr(),
					role.getPermissions().stream().map(Permission::authority).sorted().toList(), role.isOwner());
		}

	}

}
