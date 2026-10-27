package com.hrsaas.identity.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByCode(String code);

	boolean existsByCode(String code);

	List<Role> findByCodeIn(Collection<String> codes);

	List<Role> findAllByOrderById();

	@Query("select count(u) from User u join u.roles r where r = :role")
	long countUsers(Role role);

}
