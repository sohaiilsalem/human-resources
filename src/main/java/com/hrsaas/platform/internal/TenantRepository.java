package com.hrsaas.platform.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hrsaas.platform.TenantStatus;

interface TenantRepository extends JpaRepository<Tenant, Long> {

	/** Next value of the {@code tenants.id} identity sequence; must not run in a read-only transaction. */
	@Query(value = "select nextval(pg_get_serial_sequence('tenants', 'id'))", nativeQuery = true)
	long reserveId();

	Optional<Tenant> findBySlug(String slug);

	boolean existsBySlug(String slug);

	List<Tenant> findByStatus(TenantStatus status);

}
