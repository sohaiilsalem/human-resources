package com.hrsaas.leave.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.hrsaas.employee.Gender;
import com.hrsaas.leave.LeaveCategory;
import com.hrsaas.shared.persistence.TenantEntity;

@Entity
@Table(name = "leave_types")
class LeaveType extends TenantEntity {

	@Column(nullable = false, updatable = false, length = 30)
	private String code;

	@Column(nullable = false, length = 100)
	private String nameEn;

	@Column(nullable = false, length = 100)
	private String nameAr;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private LeaveCategory category;

	@Column(nullable = false)
	private boolean paid;

	@Column(nullable = false)
	private boolean tracksBalance;

	/** When set, requests consume this type's balance instead of their own. */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "deducts_from_id")
	private LeaveType deductsFrom;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DayCountMode dayCountMode;

	@Column(nullable = false)
	private boolean allowHalfDay;

	private Integer maxDaysPerRequest;

	private Integer maxDaysPerYear;

	private Integer maxTimesPerService;

	private Integer minServiceMonths;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Gender gender;

	@Column(nullable = false)
	private boolean requiresAttachment;

	@Column(nullable = false)
	private boolean active;

	/** Approval workflow; null for the company's default. */
	@Column(length = 30)
	private String workflowCode;

	protected LeaveType() {
	}

	LeaveType(LeaveTypeRequest request, LeaveType deductsFrom) {
		this.code = request.code();
		this.category = request.category();
		apply(request, deductsFrom);
	}

	void apply(LeaveTypeRequest request, LeaveType deductsFrom) {
		this.nameEn = request.nameEn().strip();
		this.nameAr = request.nameAr().strip();
		this.paid = request.paid();
		this.tracksBalance = request.tracksBalance() || deductsFrom != null;
		this.deductsFrom = deductsFrom;
		this.dayCountMode = request.dayCountMode();
		this.allowHalfDay = request.allowHalfDay();
		this.maxDaysPerRequest = request.maxDaysPerRequest();
		this.maxDaysPerYear = request.maxDaysPerYear();
		this.maxTimesPerService = request.maxTimesPerService();
		this.minServiceMonths = request.minServiceMonths();
		this.gender = request.gender();
		this.requiresAttachment = request.requiresAttachment();
		this.active = request.active();
		// omitted keeps the current workflow, so older clients don't reset it; blank means the company default
		if (request.workflowCode() != null) {
			this.workflowCode = request.workflowCode().isBlank() ? null : request.workflowCode().strip();
		}
	}

	/** The type whose ledger requests of this type draw from, or {@code null} if no balance is kept. */
	LeaveType ledgerType() {
		if (!tracksBalance) {
			return null;
		}
		return deductsFrom != null ? deductsFrom : this;
	}

	boolean hasOwnLedger() {
		return tracksBalance && deductsFrom == null;
	}

	String getCode() {
		return code;
	}

	String getNameEn() {
		return nameEn;
	}

	String getNameAr() {
		return nameAr;
	}

	LeaveCategory getCategory() {
		return category;
	}

	boolean isPaid() {
		return paid;
	}

	boolean isTracksBalance() {
		return tracksBalance;
	}

	LeaveType getDeductsFrom() {
		return deductsFrom;
	}

	DayCountMode getDayCountMode() {
		return dayCountMode;
	}

	boolean isAllowHalfDay() {
		return allowHalfDay;
	}

	Integer getMaxDaysPerRequest() {
		return maxDaysPerRequest;
	}

	Integer getMaxDaysPerYear() {
		return maxDaysPerYear;
	}

	Integer getMaxTimesPerService() {
		return maxTimesPerService;
	}

	Integer getMinServiceMonths() {
		return minServiceMonths;
	}

	Gender getGender() {
		return gender;
	}

	boolean isRequiresAttachment() {
		return requiresAttachment;
	}

	boolean isActive() {
		return active;
	}

	String getWorkflowCode() {
		return workflowCode;
	}

}
