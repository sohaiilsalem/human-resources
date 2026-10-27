package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LeaveTransactionRepository extends JpaRepository<LeaveTransaction, Long> {

	Optional<LeaveTransaction> findByEmployeeIdAndLeaveTypeIdAndLeaveYearAndKind(Long employeeId, Long leaveTypeId,
			int leaveYear, TransactionKind kind);

	/** Balance usable in {@code year}: grants of later years are not spendable yet. */
	@Query("""
			select coalesce(sum(t.days), 0) from LeaveTransaction t
			where t.employeeId = :employeeId and t.leaveTypeId = :leaveTypeId and t.leaveYear <= :year""")
	BigDecimal balance(Long employeeId, Long leaveTypeId, int year);

	@Query("""
			select t from LeaveTransaction t
			where t.employeeId = :employeeId
			order by t.effectiveDate desc, t.createdAt desc""")
	List<LeaveTransaction> history(Long employeeId);

	/**
	 * Serialises balance-changing work per employee for the rest of the transaction, so two concurrent requests
	 * cannot both spend the same days.
	 */
	@Query(value = "select 1 from (select pg_advisory_xact_lock(hashtextextended(cast(:employeeId as text), 0))) l",
			nativeQuery = true)
	Integer lockEmployee(Long employeeId);

}
