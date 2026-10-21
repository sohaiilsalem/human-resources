package com.hrsaas.shared.persistence;

import java.util.UUID;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import com.hrsaas.shared.tenant.TenantContext;

class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

	@Override
	public UUID resolveCurrentTenantIdentifier() {
		return TenantContext.current().orElse(TenantContext.NO_TENANT);
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return false;
	}

}
