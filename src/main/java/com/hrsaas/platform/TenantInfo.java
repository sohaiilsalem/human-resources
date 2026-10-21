package com.hrsaas.platform;

import java.util.UUID;

public record TenantInfo(UUID id, String slug, String name, TenantStatus status, String timeZone,
		String defaultLocale) {

	public boolean isActive() {
		return status == TenantStatus.ACTIVE;
	}

}
