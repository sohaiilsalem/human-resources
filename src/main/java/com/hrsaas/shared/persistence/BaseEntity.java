package com.hrsaas.shared.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Identity, audit columns and optimistic locking shared by every table (ARCHITECTURE §9). */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@Id
	private UUID id = Ids.newId();

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@CreatedBy
	@Column(updatable = false)
	private UUID createdBy;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	@LastModifiedBy
	private UUID updatedBy;

	@Version
	private Long version;

	public UUID getId() {
		return id;
	}

	protected void setId(UUID id) {
		this.id = id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Long getVersion() {
		return version;
	}

	@Override
	public final boolean equals(Object o) {
		return this == o || (o instanceof BaseEntity other && getClass() == other.getClass() && id.equals(other.id));
	}

	@Override
	public final int hashCode() {
		return id.hashCode();
	}

}
