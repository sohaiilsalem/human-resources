package com.hrsaas.identity.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;

/** Opaque refresh token, stored only as a SHA-256 hash and rotated on every use. */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken extends TenantEntity {

	@Column(nullable = false, updatable = false)
	private UUID userId;

	@Column(nullable = false, unique = true, updatable = false, length = 64)
	private String tokenHash;

	@Column(nullable = false, updatable = false)
	private Instant expiresAt;

	private Instant revokedAt;

	private UUID replacedBy;

	protected RefreshToken() {
	}

	RefreshToken(UUID userId, String tokenHash, Instant expiresAt) {
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	boolean isRevoked() {
		return revokedAt != null;
	}

	boolean isExpired(Instant now) {
		return !now.isBefore(expiresAt);
	}

	void revoke(Instant now, UUID replacement) {
		this.revokedAt = now;
		this.replacedBy = replacement;
	}

	UUID getUserId() {
		return userId;
	}

}
