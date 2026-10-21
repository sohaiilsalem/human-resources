package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;

@Entity
@Table(name = "leave_requests")
class LeaveRequest extends TenantEntity {

	@Column(nullable = false, updatable = false)
	private UUID employeeId;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "leave_type_id", updatable = false)
	private LeaveType leaveType;

	@Column(nullable = false, updatable = false)
	private LocalDate startDate;

	@Column(nullable = false, updatable = false)
	private LocalDate endDate;

	@Column(nullable = false, updatable = false)
	private boolean halfDay;

	@Column(nullable = false, updatable = false, precision = 6, scale = 2)
	private BigDecimal days;

	/** Leave year the days count in: the year the leave starts. */
	@Column(nullable = false, updatable = false)
	private int leaveYear;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LeaveRequestStatus status;

	@Column(length = 500)
	private String reason;

	@Column(nullable = false, updatable = false)
	private UUID requestedBy;

	private UUID decidedBy;

	private Instant decidedAt;

	@Column(length = 500)
	private String decisionNote;

	protected LeaveRequest() {
	}

	LeaveRequest(UUID employeeId, LeaveType leaveType, LocalDate startDate, LocalDate endDate, boolean halfDay,
			BigDecimal days, String reason, UUID requestedBy) {
		this.employeeId = employeeId;
		this.leaveType = leaveType;
		this.startDate = startDate;
		this.endDate = endDate;
		this.halfDay = halfDay;
		this.days = days;
		this.leaveYear = startDate.getYear();
		this.status = LeaveRequestStatus.PENDING;
		this.reason = reason;
		this.requestedBy = requestedBy;
	}

	void decide(LeaveRequestStatus outcome, UUID by, Instant at, String note) {
		this.status = outcome;
		this.decidedBy = by;
		this.decidedAt = at;
		this.decisionNote = note;
	}

	boolean isPending() {
		return status == LeaveRequestStatus.PENDING;
	}

	boolean isApproved() {
		return status == LeaveRequestStatus.APPROVED;
	}

	UUID getEmployeeId() {
		return employeeId;
	}

	LeaveType getLeaveType() {
		return leaveType;
	}

	LocalDate getStartDate() {
		return startDate;
	}

	LocalDate getEndDate() {
		return endDate;
	}

	boolean isHalfDay() {
		return halfDay;
	}

	BigDecimal getDays() {
		return days;
	}

	int getLeaveYear() {
		return leaveYear;
	}

	LeaveRequestStatus getStatus() {
		return status;
	}

	String getReason() {
		return reason;
	}

	UUID getRequestedBy() {
		return requestedBy;
	}

	UUID getDecidedBy() {
		return decidedBy;
	}

	Instant getDecidedAt() {
		return decidedAt;
	}

	String getDecisionNote() {
		return decisionNote;
	}

}
