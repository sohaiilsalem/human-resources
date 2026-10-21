package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

	/** Pending days that will be drawn from {@code ledgerTypeId}'s balance, directly or via a deducting type. */
	@Query("""
			select coalesce(sum(r.days), 0) from LeaveRequest r join r.leaveType lt left join lt.deductsFrom df
			where r.employeeId = :employeeId and r.status = com.hrsaas.leave.internal.LeaveRequestStatus.PENDING
			  and (lt.id = :ledgerTypeId or df.id = :ledgerTypeId)""")
	BigDecimal pendingDays(UUID employeeId, UUID ledgerTypeId);

	@Query("""
			select coalesce(sum(r.days), 0) from LeaveRequest r
			where r.employeeId = :employeeId and r.leaveType.id = :leaveTypeId and r.leaveYear = :year
			  and r.status in :statuses""")
	BigDecimal daysInYear(UUID employeeId, UUID leaveTypeId, int year, Collection<LeaveRequestStatus> statuses);

	long countByEmployeeIdAndLeaveTypeIdAndStatusIn(UUID employeeId, UUID leaveTypeId,
			Collection<LeaveRequestStatus> statuses);

	@Query("""
			select count(r) > 0 from LeaveRequest r
			where r.employeeId = :employeeId and r.status in :statuses
			  and r.startDate <= :endDate and r.endDate >= :startDate""")
	boolean overlaps(UUID employeeId, LocalDate startDate, LocalDate endDate,
			Collection<LeaveRequestStatus> statuses);

	Page<LeaveRequest> findByEmployeeId(UUID employeeId, Pageable pageable);

	@Query("""
			select r from LeaveRequest r
			where (:status is null or r.status = :status) and (:employeeId is null or r.employeeId = :employeeId)""")
	Page<LeaveRequest> search(LeaveRequestStatus status, UUID employeeId, Pageable pageable);

	@Query("""
			select r from LeaveRequest r
			where r.employeeId in :employeeIds and (:status is null or r.status = :status)""")
	Page<LeaveRequest> searchAmong(Collection<UUID> employeeIds, LeaveRequestStatus status, Pageable pageable);

}
