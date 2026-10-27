package com.hrsaas.platform;

import java.util.List;
import java.util.Optional;

public interface TenantDirectory {

	Optional<TenantInfo> findBySlug(String slug);

	Optional<TenantInfo> findById(Long id);

	/** For platform jobs that iterate over tenants. */
	List<TenantInfo> findAllActive();

	/** A fresh tenant id for {@link #register}; ids lost to a failed sign-up leave a gap in the sequence. */
	long reserveId();

	/** @throws com.hrsaas.shared.web.ApiException {@code tenant.slug-taken} if the slug is in use */
	TenantInfo register(RegisterTenant command);

}
