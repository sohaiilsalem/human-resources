package com.hrsaas.identity.internal;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import com.hrsaas.identity.Role;
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

	private UUID employeeId;

	private Instant lastLoginAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 30)
	private Set<Role> roles = EnumSet.noneOf(Role.class);

	protected User() {
	}

	User(String email, String fullName, String passwordHash, String locale, Set<Role> roles, UUID employeeId) {
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

	UUID getEmployeeId() {
		return employeeId;
	}

	String getLocale() {
		return locale;
	}

	Set<Role> getRoles() {
		return Set.copyOf(roles);
	}

}
