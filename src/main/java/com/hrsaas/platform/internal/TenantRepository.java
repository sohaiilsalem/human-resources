package com.hrsaas.platform.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrsaas.platform.TenantStatus;

interface TenantRepository extends JpaRepository<Tenant, UUID> {

	Optional<Tenant> findBySlug(String slug);

	boolean existsBySlug(String slug);

	List<Tenant> findByStatus(TenantStatus status);

}
