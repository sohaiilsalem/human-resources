package com.hrsaas.platform.internal;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.hrsaas.platform.TenantInfo;
import com.hrsaas.platform.TenantStatus;
import com.hrsaas.shared.persistence.BaseEntity;

@Entity
@Table(name = "tenants")
class Tenant extends BaseEntity {

	@Column(nullable = false, unique = true, length = 40)
	private String slug;

	@Column(nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TenantStatus status;

	@Column(nullable = false, length = 50)
	private String timeZone;

	@Column(nullable = false, length = 5)
	private String defaultLocale;

	protected Tenant() {
	}

	Tenant(UUID id, String slug, String name, String defaultLocale) {
		setId(id);
		this.slug = slug;
		this.name = name;
		this.status = TenantStatus.ACTIVE;
		this.timeZone = "Africa/Cairo";
		this.defaultLocale = defaultLocale;
	}

	TenantInfo toInfo() {
		return new TenantInfo(getId(), slug, name, status, timeZone, defaultLocale);
	}

}
