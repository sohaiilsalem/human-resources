package com.hrsaas.workflow.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;
import com.hrsaas.workflow.ApprovalActionType;

/** Audit trail entry. Insert-only: the application role has no UPDATE or DELETE on the table (V7). */
@Entity
@Table(name = "approval_actions")
class ApprovalAction extends TenantEntity {

	@Column(nullable = false, updatable = false)
	private Long instanceId;

	@Column(updatable = false)
	private Long stepId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private ApprovalActionType action;

	/** Null for what the system did by itself (assignment, escalation, skipping). */
	@Column(updatable = false)
	private Long actorUserId;

	@Column(updatable = false, length = 500)
	private String note;

	protected ApprovalAction() {
	}

	ApprovalAction(Long instanceId, Long stepId, ApprovalActionType action, Long actorUserId, String note) {
		this.instanceId = instanceId;
		this.stepId = stepId;
		this.action = action;
		this.actorUserId = actorUserId;
		this.note = note;
	}

	Long getInstanceId() {
		return instanceId;
	}

	Long getStepId() {
		return stepId;
	}

	ApprovalActionType getAction() {
		return action;
	}

	Long getActorUserId() {
		return actorUserId;
	}

	String getNote() {
		return note;
	}

}
