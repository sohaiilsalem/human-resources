package com.hrsaas.platform;

public record TenantInfo(Long id, String slug, String name, TenantStatus status, String timeZone,
		String defaultLocale) {

	public boolean isActive() {
		return status == TenantStatus.ACTIVE;
	}

}
