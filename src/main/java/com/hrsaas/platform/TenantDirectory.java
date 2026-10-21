package com.hrsaas.platform;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantDirectory {

	Optional<TenantInfo> findBySlug(String slug);

	Optional<TenantInfo> findById(UUID id);

	/** For platform jobs that iterate over tenants. */
	List<TenantInfo> findAllActive();

	/** @throws com.hrsaas.shared.web.ApiException {@code tenant.slug-taken} if the slug is in use */
	TenantInfo register(RegisterTenant command);

}
