package com.hrsaas.identity.internal;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.identity.UserAccounts;
import com.hrsaas.shared.web.ApiException;

@Service
@Transactional(readOnly = true)
class UserService implements UserAccounts {

	private final UserRepository users;

	private final EmployeeDirectory employees;

	private final RoleService roles;

	private final PasswordEncoder passwordEncoder;

	UserService(UserRepository users, EmployeeDirectory employees, RoleService roles,
			PasswordEncoder passwordEncoder) {
		this.users = users;
		this.employees = employees;
		this.roles = roles;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public Optional<Long> employeeIdOf(Long userId) {
		return users.findById(userId).map(User::getEmployeeId);
	}

	List<User> list() {
		return users.findAllByOrderByFullName();
	}

	/**
	 * Creates an account with an initial password set by an administrator. Invitation e-mails (set-your-own password)
	 * come with the notification module.
	 */
	@Transactional
	User create(CreateUser request) {
		String email = request.email().strip().toLowerCase(Locale.ROOT);
		if (users.existsByEmail(email)) {
			throw ApiException.conflict("user.email-taken", email);
		}
		Set<Role> granted = roles.resolveGrantable(request.roles());
		if (request.employeeId() != null) {
			if (employees.findById(request.employeeId()).isEmpty()) {
				throw ApiException.unprocessable("employee.not-found");
			}
			if (users.existsByEmployeeId(request.employeeId())) {
				throw ApiException.conflict("user.employee-already-linked");
			}
		}
		String locale = request.locale() != null ? request.locale() : "en";
		return users.save(new User(email, request.fullName().strip(), passwordEncoder.encode(request.password()),
				locale, granted, request.employeeId()));
	}

	record CreateUser(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 200) String fullName,
			@NotBlank @Size(min = 8, max = 128) String password, @NotEmpty Set<String> roles, Long employeeId,
			@Pattern(regexp = "en|ar", message = "{validation.locale}") String locale) {
	}

}
