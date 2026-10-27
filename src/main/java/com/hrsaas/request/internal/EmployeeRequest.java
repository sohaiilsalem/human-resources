package com.hrsaas.request.internal;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.hrsaas.shared.persistence.TenantEntity;

/** A filed request. The form values are validated against the type's fields at submission and then kept as filed. */
@Entity
@Table(name = "employee_requests")
class EmployeeRequest extends TenantEntity {

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "request_type_id", updatable = false)
	private RequestType type;

	@Column(nullable = false, updatable = false)
	private Long employeeId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RequestStatus status;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, updatable = false)
	private Map<String, Object> data = new LinkedHashMap<>();

	@Column(updatable = false, length = 1000)
	private String reason;

	@Column(nullable = false, updatable = false)
	private Long requestedBy;

	private Instant decidedAt;

	@Column(length = 500)
	private String decisionNote;

	protected EmployeeRequest() {
	}

	EmployeeRequest(RequestType type, Long employeeId, Map<String, Object> data, String reason, Long requestedBy) {
		this.type = type;
		this.employeeId = employeeId;
		this.status = RequestStatus.PENDING;
		this.data = new LinkedHashMap<>(data);
		this.reason = reason;
		this.requestedBy = requestedBy;
	}

	void close(RequestStatus outcome, Instant at, String note) {
		this.status = outcome;
		this.decidedAt = at;
		this.decisionNote = note;
	}

	boolean isPending() {
		return status == RequestStatus.PENDING;
	}

	boolean isApproved() {
		return status == RequestStatus.APPROVED;
	}

	RequestType getType() {
		return type;
	}

	Long getEmployeeId() {
		return employeeId;
	}

	RequestStatus getStatus() {
		return status;
	}

	Map<String, Object> getData() {
		return Map.copyOf(data);
	}

	String getReason() {
		return reason;
	}

	Long getRequestedBy() {
		return requestedBy;
	}

	Instant getDecidedAt() {
		return decidedAt;
	}

	String getDecisionNote() {
		return decisionNote;
	}

}
