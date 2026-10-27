package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;

/** Balance ledger row: a balance is the sum of its rows, never a mutable counter. */
@Entity
@Table(name = "leave_transactions")
class LeaveTransaction extends TenantEntity {

	@Column(nullable = false, updatable = false)
	private Long employeeId;

	@Column(nullable = false, updatable = false)
	private Long leaveTypeId;

	@Column(nullable = false, updatable = false)
	private int leaveYear;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private TransactionKind kind;

	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal days;

	@Column(nullable = false)
	private LocalDate effectiveDate;

	@Column(updatable = false)
	private Long requestId;

	@Column(length = 500)
	private String note;

	protected LeaveTransaction() {
	}

	LeaveTransaction(Long employeeId, Long leaveTypeId, int leaveYear, TransactionKind kind, BigDecimal days,
			LocalDate effectiveDate, Long requestId, String note) {
		this.employeeId = employeeId;
		this.leaveTypeId = leaveTypeId;
		this.leaveYear = leaveYear;
		this.kind = kind;
		this.days = days;
		this.effectiveDate = effectiveDate;
		this.requestId = requestId;
		this.note = note;
	}

	/** Only entitlement rows are re-synchronised when rules or employee data change. */
	void resync(BigDecimal days, LocalDate effectiveDate) {
		this.days = days;
		this.effectiveDate = effectiveDate;
	}

	Long getLeaveTypeId() {
		return leaveTypeId;
	}

	int getLeaveYear() {
		return leaveYear;
	}

	TransactionKind getKind() {
		return kind;
	}

	BigDecimal getDays() {
		return days;
	}

	LocalDate getEffectiveDate() {
		return effectiveDate;
	}

	Long getRequestId() {
		return requestId;
	}

	String getNote() {
		return note;
	}

}
