package com.hrsaas.platform.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.platform.RegisterTenant;
import com.hrsaas.platform.TenantDirectory;
import com.hrsaas.platform.TenantInfo;
import com.hrsaas.platform.TenantRegistered;
import com.hrsaas.platform.TenantStatus;
import com.hrsaas.shared.web.ApiException;

@Service
@Transactional(readOnly = true)
class TenantService implements TenantDirectory {

	private final TenantRepository tenants;

	private final ApplicationEventPublisher events;

	TenantService(TenantRepository tenants, ApplicationEventPublisher events) {
		this.tenants = tenants;
		this.events = events;
	}

	@Override
	public Optional<TenantInfo> findBySlug(String slug) {
		return tenants.findBySlug(slug).map(Tenant::toInfo);
	}

	@Override
	public Optional<TenantInfo> findById(UUID id) {
		return tenants.findById(id).map(Tenant::toInfo);
	}

	@Override
	public List<TenantInfo> findAllActive() {
		return tenants.findByStatus(TenantStatus.ACTIVE).stream().map(Tenant::toInfo).toList();
	}

	@Override
	@Transactional
	public TenantInfo register(RegisterTenant command) {
		if (tenants.existsBySlug(command.slug())) {
			throw ApiException.conflict("tenant.slug-taken", command.slug());
		}
		Tenant tenant = tenants.save(new Tenant(command.id(), command.slug(), command.name(), command.defaultLocale()));
		events.publishEvent(new TenantRegistered(tenant.getId(), command.slug(), command.name()));
		return tenant.toInfo();
	}

}
