package com.hrsaas.identity.internal;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import com.hrsaas.identity.Permission;
import com.hrsaas.shared.persistence.TenantEntity;

@Entity
@Table(name = "users")
class User extends TenantEntity {

	@Column(nullable = false, length = 254)
	private String email;

	@Column(nullable = false, length = 200)
	private String fullName;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false, length = 5)
	private String locale;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	private Long employeeId;

	private Instant lastLoginAt;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	protected User() {
	}

	User(String email, String fullName, String passwordHash, String locale, Set<Role> roles, Long employeeId) {
		this.email = email;
		this.fullName = fullName;
		this.passwordHash = passwordHash;
		this.locale = locale;
		this.status = UserStatus.ACTIVE;
		this.roles.addAll(roles);
		this.employeeId = employeeId;
	}

	boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	void recordLogin(Instant at) {
		this.lastLoginAt = at;
	}

	String getEmail() {
		return email;
	}

	String getFullName() {
		return fullName;
	}

	String getPasswordHash() {
		return passwordHash;
	}

	Long getEmployeeId() {
		return employeeId;
	}

	String getLocale() {
		return locale;
	}

	Set<Role> getRoles() {
		return Set.copyOf(roles);
	}

	/** Role codes, sorted, as they appear in tokens and responses. */
	List<String> getRoleCodes() {
		return roles.stream().map(Role::getCode).sorted().toList();
	}

	/** Union of the permissions of all roles. */
	Set<Permission> getPermissions() {
		Set<Permission> permissions = EnumSet.noneOf(Permission.class);
		roles.forEach(role -> permissions.addAll(role.getPermissions()));
		return permissions;
	}

}
