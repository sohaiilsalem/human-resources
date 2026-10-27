package com.hrsaas.workflow.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;

/** A named approval chain, chosen by request types and leave types through its code. */
@Entity
@Table(name = "approval_workflows")
class ApprovalWorkflow extends TenantEntity {

	@Column(nullable = false, updatable = false, length = 30)
	private String code;

	@Column(nullable = false, length = 100)
	private String nameEn;

	@Column(nullable = false, length = 100)
	private String nameAr;

	@Column(length = 500)
	private String description;

	@Column(name = "is_default", nullable = false)
	private boolean defaultWorkflow;

	@Column(nullable = false)
	private boolean active;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "approval_workflow_steps", joinColumns = @JoinColumn(name = "workflow_id"))
	@OrderColumn(name = "position")
	private List<WorkflowStep> steps = new ArrayList<>();

	protected ApprovalWorkflow() {
	}

	ApprovalWorkflow(String code, String nameEn, String nameAr, String description, List<WorkflowStep> steps) {
		this.code = code;
		this.active = true;
		apply(nameEn, nameAr, description, true, steps);
	}

	/** Steps are kept sorted by stage, so the stored position is also the order they appear in. */
	void apply(String nameEn, String nameAr, String description, boolean active, List<WorkflowStep> steps) {
		this.nameEn = nameEn.strip();
		this.nameAr = nameAr.strip();
		this.description = description == null || description.isBlank() ? null : description.strip();
		this.active = active;
		this.steps.clear();
		this.steps.addAll(steps.stream().sorted(Comparator.comparingInt(WorkflowStep::getStage)).toList());
	}

	void markDefault(boolean value) {
		this.defaultWorkflow = value;
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

	String getDescription() {
		return description;
	}

	boolean isDefault() {
		return defaultWorkflow;
	}

	boolean isActive() {
		return active;
	}

	List<WorkflowStep> getSteps() {
		return List.copyOf(steps);
	}

}
