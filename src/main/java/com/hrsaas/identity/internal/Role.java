package com.hrsaas.identity.internal;

import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import com.hrsaas.identity.Permission;
import com.hrsaas.shared.persistence.TenantEntity;

/**
 * A named set of permissions, defined per tenant. {@value #OWNER} is created at sign-up, always holds every
 * permission and can be neither changed, deleted nor granted.
 */
@Entity
@Table(name = "roles")
class Role extends TenantEntity {

	static final String OWNER = "OWNER";

	@Column(nullable = false, updatable = false, length = 30)
	private String code;

	@Column(nullable = false, length = 100)
	private String nameEn;

	@Column(nullable = false, length = 100)
	private String nameAr;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "permission", nullable = false, length = 40)
	@Convert(converter = PermissionConverter.class)
	private Set<Permission> permissions = EnumSet.noneOf(Permission.class);

	protected Role() {
	}

	Role(String code, String nameEn, String nameAr, Set<Permission> permissions) {
		this.code = code;
		apply(nameEn, nameAr, permissions);
	}

	void apply(String nameEn, String nameAr, Set<Permission> permissions) {
		this.nameEn = nameEn.strip();
		this.nameAr = nameAr.strip();
		this.permissions.clear();
		if (!isOwner()) {
			this.permissions.addAll(permissions);
		}
	}

	boolean isOwner() {
		return OWNER.equals(code);
	}

	String getCode() {
		return code;
	}

	String getNameEn() {
		return nameEn;
	}

	String getNameAr() {
		return nameAr;
	}

	Set<Permission> getPermissions() {
		return isOwner() ? EnumSet.allOf(Permission.class) : Set.copyOf(permissions);
	}

}
