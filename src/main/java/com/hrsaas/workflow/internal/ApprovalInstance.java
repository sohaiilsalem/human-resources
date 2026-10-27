package com.hrsaas.workflow.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;
import com.hrsaas.workflow.ApprovalStatus;
import com.hrsaas.workflow.ApprovalStepStatus;
import com.hrsaas.workflow.StartApproval;

/** The approval of one subject: its copied steps, which stage is active, and the outcome. */
@Entity
@Table(name = "approval_instances")
class ApprovalInstance extends TenantEntity {

	@Column(nullable = false, updatable = false, length = 20)
	private String subjectType;

	@Column(nullable = false, updatable = false)
	private Long subjectId;

	@Column(nullable = false, updatable = false)
	private Long employeeId;

	@Column(nullable = false, updatable = false, length = 30)
	private String workflowCode;

	@Column(nullable = false, length = 300)
	private String summaryEn;

	@Column(nullable = false, length = 300)
	private String summaryAr;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApprovalStatus status;

	/** Stage number (as defined in the workflow) currently being decided; null before the first activation. */
	private Integer currentStage;

	@Column(nullable = false, updatable = false)
	private Long requestedBy;

	private Instant completedAt;

	@OneToMany(mappedBy = "instance", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	@OrderBy("position")
	private List<ApprovalStep> steps = new ArrayList<>();

	protected ApprovalInstance() {
	}

	ApprovalInstance(StartApproval command, String workflowCode) {
		this.subjectType = command.subjectType();
		this.subjectId = command.subjectId();
		this.employeeId = command.employeeId();
		this.workflowCode = workflowCode;
		this.summaryEn = truncate(command.summaryEn());
		this.summaryAr = truncate(command.summaryAr());
		this.status = ApprovalStatus.IN_PROGRESS;
		this.requestedBy = command.requestedBy();
	}

	ApprovalStep addStep(WorkflowStep definition) {
		ApprovalStep step = new ApprovalStep(this, steps.size(), definition);
		steps.add(step);
		return step;
	}

	/** The lowest stage after the current one that still has waiting steps. */
	OptionalInt nextStage() {
		int after = currentStage == null ? Integer.MIN_VALUE : currentStage;
		return steps.stream()
			.filter(step -> step.getStatus() == ApprovalStepStatus.WAITING && step.getStage() > after)
			.mapToInt(ApprovalStep::getStage)
			.min();
	}

	void activate(int stage) {
		this.currentStage = stage;
	}

	List<ApprovalStep> currentSteps() {
		return steps.stream().filter(step -> currentStage != null && step.getStage() == currentStage).toList();
	}

	/** Every step of the active stage is approved or skipped. */
	boolean currentStageDone() {
		return currentSteps().stream()
			.allMatch(step -> step.getStatus() == ApprovalStepStatus.APPROVED
					|| step.getStatus() == ApprovalStepStatus.SKIPPED);
	}

	boolean anyApproved() {
		return steps.stream().anyMatch(step -> step.getStatus() == ApprovalStepStatus.APPROVED);
	}

	void complete(ApprovalStatus outcome, Instant at) {
		this.status = outcome;
		this.completedAt = at;
		steps.forEach(ApprovalStep::cancelIfOpen);
	}

	boolean isInProgress() {
		return status == ApprovalStatus.IN_PROGRESS;
	}

	private static String truncate(String value) {
		return value.length() <= 300 ? value : value.substring(0, 299) + "…";
	}

	String getSubjectType() {
		return subjectType;
	}

	Long getSubjectId() {
		return subjectId;
	}

	Long getEmployeeId() {
		return employeeId;
	}

	String getWorkflowCode() {
		return workflowCode;
	}

	String getSummaryEn() {
		return summaryEn;
	}

	String getSummaryAr() {
		return summaryAr;
	}

	ApprovalStatus getStatus() {
		return status;
	}

	Integer getCurrentStage() {
		return currentStage;
	}

	Long getRequestedBy() {
		return requestedBy;
	}

	Instant getCompletedAt() {
		return completedAt;
	}

	List<ApprovalStep> getSteps() {
		return List.copyOf(steps);
	}

}
