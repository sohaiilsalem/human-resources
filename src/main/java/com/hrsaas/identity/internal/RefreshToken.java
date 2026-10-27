package com.hrsaas.identity.internal;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;

/** Opaque refresh token, stored only as a SHA-256 hash and rotated on every use. */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken extends TenantEntity {

	@Column(nullable = false, updatable = false)
	private Long userId;

	@Column(nullable = false, unique = true, updatable = false, length = 64)
	private String tokenHash;

	@Column(nullable = false, updatable = false)
	private Instant expiresAt;

	private Instant revokedAt;

	private Long replacedBy;

	protected RefreshToken() {
	}

	RefreshToken(Long userId, String tokenHash, Instant expiresAt) {
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

	void revoke(Instant now, Long replacement) {
		this.revokedAt = now;
		this.replacedBy = replacement;
	}

	Long getUserId() {
		return userId;
	}

}
