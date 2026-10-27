package com.hrsaas.shared.persistence;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import com.hrsaas.shared.tenant.TenantContext;

class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

	@Override
	public Long resolveCurrentTenantIdentifier() {
		return TenantContext.current().orElse(TenantContext.NO_TENANT);
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return false;
	}

}
