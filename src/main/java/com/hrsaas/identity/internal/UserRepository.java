package com.hrsaas.identity.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Queries are restricted to the current tenant by Hibernate {@code @TenantId} and row-level security. */
interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByEmployeeId(Long employeeId);

	List<User> findAllByOrderByFullName();

	Optional<User> findByEmployeeId(Long employeeId);

	@Query("""
			select u.id from User u join u.roles r
			where r.code = :roleCode and u.status = com.hrsaas.identity.internal.UserStatus.ACTIVE
			order by u.id""")
	List<Long> findActiveIdsWithRole(String roleCode);

}
