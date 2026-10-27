package com.hrsaas.identity.internal;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAuthority('user:manage')")
class UserController {

	private final UserService users;

	UserController(UserService users) {
		this.users = users;
	}

	@GetMapping
	List<UserResponse> list() {
		return users.list().stream().map(UserResponse::of).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse create(@Valid @RequestBody UserService.CreateUser request) {
		return UserResponse.of(users.create(request));
	}

	record UserResponse(Long id, String email, String fullName, String locale, List<String> roles, Long employeeId,
			boolean active) {

		static UserResponse of(User user) {
			return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getLocale(),
					user.getRoleCodes(), user.getEmployeeId(), user.isActive());
		}

	}

}
