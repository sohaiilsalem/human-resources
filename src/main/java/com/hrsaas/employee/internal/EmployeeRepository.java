package com.hrsaas.employee.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hrsaas.employee.EmployeeStatus;

interface EmployeeRepository extends JpaRepository<Employee, UUID> {

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, UUID id);

	Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

	@Query("select e.id from Employee e where e.status = com.hrsaas.employee.EmployeeStatus.ACTIVE")
	List<UUID> findActiveIds();

	@Query("select e.id from Employee e where e.managerId = :managerId")
	List<UUID> findDirectReportIds(UUID managerId);

}
