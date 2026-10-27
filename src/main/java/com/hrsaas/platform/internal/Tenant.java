package com.hrsaas.platform.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.hrsaas.platform.TenantInfo;
import com.hrsaas.platform.TenantStatus;
import com.hrsaas.shared.persistence.AuditedEntity;

/**
 * The id is reserved up front ({@link TenantRepository#reserveId}) so tenant-scoped rows can be written in the same
 * transaction.
 */
@Entity
@Table(name = "tenants")
class Tenant extends AuditedEntity {

	@Id
	private Long id;

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

	Tenant(Long id, String slug, String name, String defaultLocale) {
		this.id = id;
		this.slug = slug;
		this.name = name;
		this.status = TenantStatus.ACTIVE;
		this.timeZone = "Africa/Cairo";
		this.defaultLocale = defaultLocale;
	}

	@Override
	public Long getId() {
		return id;
	}

	TenantInfo toInfo() {
		return new TenantInfo(getId(), slug, name, status, timeZone, defaultLocale);
	}

}
