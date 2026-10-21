package com.hrsaas.identity.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Queries are restricted to the current tenant by Hibernate {@code @TenantId} and row-level security. */
interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByEmployeeId(UUID employeeId);

	List<User> findAllByOrderByFullName();

}
