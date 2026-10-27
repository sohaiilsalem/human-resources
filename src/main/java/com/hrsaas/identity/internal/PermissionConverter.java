package com.hrsaas.identity.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.hrsaas.identity.Permission;

/** Stores a permission as its authority ({@code employee:read}), the same string endpoints check. */
@Converter
class PermissionConverter implements AttributeConverter<Permission, String> {

	@Override
	public String convertToDatabaseColumn(Permission permission) {
		return permission.authority();
	}

	@Override
	public Permission convertToEntityAttribute(String authority) {
		return Permission.fromAuthority(authority)
			.orElseThrow(() -> new IllegalStateException("Unknown permission in role_permissions: " + authority));
	}

}
