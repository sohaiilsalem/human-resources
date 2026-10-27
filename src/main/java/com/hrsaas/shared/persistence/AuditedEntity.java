package com.hrsaas.shared.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Audit columns and optimistic locking shared by every table (ARCHITECTURE §9). Subclasses map the {@code id};
 * almost all of them through {@link BaseEntity}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditedEntity {

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@CreatedBy
	@Column(updatable = false)
	private Long createdBy;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	@LastModifiedBy
	private Long updatedBy;

	@Version
	private Long version;

	public abstract Long getId();

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Long getVersion() {
		return version;
	}

	/** Entities without an id yet (not persisted) are only equal to themselves. */
	@Override
	public final boolean equals(Object o) {
		return this == o || (o instanceof AuditedEntity other && getClass() == other.getClass() && getId() != null
				&& getId().equals(other.getId()));
	}

	@Override
	public final int hashCode() {
		return getClass().hashCode();
	}

}
