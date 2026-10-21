package com.hrsaas.shared.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.TenantId;

/**
 * Base for tenant-owned tables. Hibernate fills {@code tenant_id} on insert and filters every query by it;
 * PostgreSQL row-level security enforces the same rule underneath.
 */
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

	@TenantId
	@Column(name = "tenant_id", nullable = false, updatable = false)
	private UUID tenantId;

	public UUID getTenantId() {
		return tenantId;
	}

}
