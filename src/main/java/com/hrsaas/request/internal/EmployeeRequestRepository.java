package com.hrsaas.request.internal;

import java.time.Instant;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface EmployeeRequestRepository extends JpaRepository<EmployeeRequest, Long> {

	Page<EmployeeRequest> findByEmployeeId(Long employeeId, Pageable pageable);

	@Query("""
			select r from EmployeeRequest r
			where (:status is null or r.status = :status) and (:type is null or r.type.code = :type)
			  and (:employeeId is null or r.employeeId = :employeeId)""")
	Page<EmployeeRequest> search(RequestStatus status, String type, Long employeeId, Pageable pageable);

	@Query("""
			select count(r) from EmployeeRequest r
			where r.employeeId = :employeeId and r.type.id = :typeId and r.status in :statuses
			  and r.createdAt >= :from and r.createdAt < :to""")
	long countInPeriod(Long employeeId, Long typeId, Collection<RequestStatus> statuses, Instant from, Instant to);

}
