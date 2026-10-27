package com.hrsaas.workflow.internal;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;
import com.hrsaas.workflow.ApprovalStepStatus;

/**
 * A step of a running approval, copied from the workflow at submission. Assigned to one user
 * ({@code assigneeUserId}) or to everyone holding a role ({@code assigneeRoleCode}) when its stage becomes active.
 */
@Entity
@Table(name = "approval_steps")
class ApprovalStep extends TenantEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "instance_id", updatable = false)
	private ApprovalInstance instance;

	@Column(nullable = false, updatable = false)
	private int stage;

	@Column(nullable = false, updatable = false)
	private int position;

	@Column(nullable = false, updatable = false, length = 100)
	private String nameEn;

	@Column(nullable = false, updatable = false, length = 100)
	private String nameAr;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private ApproverType approverType;

	@Column(updatable = false)
	private Integer managerLevel;

	@Column(updatable = false, length = 30)
	private String roleCode;

	@Column(updatable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "if_unresolved", nullable = false, updatable = false, length = 20)
	private UnresolvedPolicy ifUnresolved;

	@Column(updatable = false, length = 30)
	private String fallbackRoleCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApprovalStepStatus status;

	private Long assigneeUserId;

	@Column(length = 30)
	private String assigneeRoleCode;

	private Long decidedBy;

	private Instant decidedAt;

	@Column(length = 500)
	private String note;

	protected ApprovalStep() {
	}

	ApprovalStep(ApprovalInstance instance, int position, WorkflowStep definition) {
		this.instance = instance;
		this.position = position;
		this.stage = definition.getStage();
		this.nameEn = definition.getNameEn();
		this.nameAr = definition.getNameAr();
		this.approverType = definition.getApproverType();
		this.managerLevel = definition.getManagerLevel();
		this.roleCode = definition.getRoleCode();
		this.userId = definition.getUserId();
		this.ifUnresolved = definition.getIfUnresolved();
		this.fallbackRoleCode = definition.getFallbackRoleCode();
		this.status = ApprovalStepStatus.WAITING;
	}

	void assignUser(Long userId) {
		this.assigneeUserId = userId;
		this.assigneeRoleCode = null;
		this.status = ApprovalStepStatus.PENDING;
	}

	void assignRole(String roleCode) {
		this.assigneeUserId = null;
		this.assigneeRoleCode = roleCode;
		this.status = ApprovalStepStatus.PENDING;
	}

	void skip() {
		this.status = ApprovalStepStatus.SKIPPED;
	}

	void decide(ApprovalStepStatus outcome, Long by, Instant at, String note) {
		this.status = outcome;
		this.decidedBy = by;
		this.decidedAt = at;
		this.note = note;
	}

	void cancelIfOpen() {
		if (status == ApprovalStepStatus.WAITING || status == ApprovalStepStatus.PENDING) {
			status = ApprovalStepStatus.CANCELLED;
		}
	}

	boolean isPending() {
		return status == ApprovalStepStatus.PENDING;
	}

	String fallbackRole() {
		return fallbackRoleCode != null ? fallbackRoleCode : WorkflowStep.DEFAULT_FALLBACK_ROLE;
	}

	ApprovalInstance getInstance() {
		return instance;
	}

	int getStage() {
		return stage;
	}

	int getPosition() {
		return position;
	}

	String getNameEn() {
		return nameEn;
	}

	String getNameAr() {
		return nameAr;
	}

	ApproverType getApproverType() {
		return approverType;
	}

	Integer getManagerLevel() {
		return managerLevel;
	}

	String getRoleCode() {
		return roleCode;
	}

	Long getUserId() {
		return userId;
	}

	UnresolvedPolicy getIfUnresolved() {
		return ifUnresolved;
	}

	ApprovalStepStatus getStatus() {
		return status;
	}

	Long getAssigneeUserId() {
		return assigneeUserId;
	}

	String getAssigneeRoleCode() {
		return assigneeRoleCode;
	}

	Long getDecidedBy() {
		return decidedBy;
	}

	Instant getDecidedAt() {
		return decidedAt;
	}

	String getNote() {
		return note;
	}

}
