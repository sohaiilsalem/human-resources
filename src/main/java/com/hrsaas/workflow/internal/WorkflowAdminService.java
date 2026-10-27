package com.hrsaas.workflow.internal;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.identity.UserAccounts;
import com.hrsaas.shared.web.ApiException;

/**
 * Workflow configuration. Editing a workflow affects requests submitted afterwards only; running approvals keep the
 * steps they were started with. Workflows are deactivated rather than deleted, since request and leave types refer
 * to them by code.
 */
@Service
@Transactional
class WorkflowAdminService {

	private final ApprovalWorkflowRepository workflows;

	private final UserAccounts users;

	WorkflowAdminService(ApprovalWorkflowRepository workflows, UserAccounts users) {
		this.workflows = workflows;
		this.users = users;
	}

	@Transactional(readOnly = true)
	List<ApprovalWorkflow> list() {
		return workflows.findAllByOrderById();
	}

	@Transactional(readOnly = true)
	ApprovalWorkflow get(String code) {
		return workflows.findByCode(code).orElseThrow(() -> ApiException.notFound("workflow.not-found", code));
	}

	ApprovalWorkflow create(WorkflowRequest request) {
		if (workflows.existsByCode(request.code())) {
			throw ApiException.conflict("workflow.code-taken", request.code());
		}
		List<WorkflowStep> steps = steps(request);
		ApprovalWorkflow workflow = new ApprovalWorkflow(request.code(), request.nameEn(), request.nameAr(),
				request.description(), steps);
		workflow.apply(request.nameEn(), request.nameAr(), request.description(), request.active(), steps);
		return workflows.save(workflow);
	}

	ApprovalWorkflow update(String code, WorkflowRequest request) {
		ApprovalWorkflow workflow = get(code);
		if (!workflow.getCode().equals(request.code())) {
			throw ApiException.unprocessable("workflow.code-immutable");
		}
		if (workflow.isDefault() && !request.active()) {
			throw ApiException.unprocessable("workflow.default-must-be-active");
		}
		workflow.apply(request.nameEn(), request.nameAr(), request.description(), request.active(), steps(request));
		return workflow;
	}

	/** The workflow used when a request or leave type names none (or names one that is inactive). */
	ApprovalWorkflow makeDefault(String code) {
		ApprovalWorkflow workflow = get(code);
		if (!workflow.isActive()) {
			throw ApiException.unprocessable("workflow.default-must-be-active");
		}
		workflows.findByDefaultWorkflowTrue().filter(current -> !current.equals(workflow)).ifPresent(current -> {
			current.markDefault(false);
			workflows.flush();
		});
		workflow.markDefault(true);
		return workflow;
	}

	private List<WorkflowStep> steps(WorkflowRequest request) {
		return request.steps().stream().map(this::step).toList();
	}

	private WorkflowStep step(StepRequest step) {
		switch (step.approverType()) {
			case MANAGER_LEVEL -> {
				if (step.managerLevel() == null || step.managerLevel() < 2) {
					throw ApiException.unprocessable("workflow.manager-level-required");
				}
			}
			case ROLE -> requireRole(step.roleCode());
			case USER -> {
				if (step.userId() == null || !users.isActive(step.userId())) {
					throw ApiException.unprocessable("workflow.user-not-found");
				}
			}
			case DIRECT_MANAGER -> {
			}
		}
		boolean anyCondition = step.conditionField() != null || step.conditionOperator() != null
				|| step.conditionValue() != null;
		boolean fullCondition = step.conditionField() != null && step.conditionOperator() != null
				&& step.conditionValue() != null;
		if (anyCondition && !fullCondition) {
			throw ApiException.unprocessable("workflow.incomplete-condition");
		}
		if (step.fallbackRoleCode() != null) {
			requireRole(step.fallbackRoleCode());
		}
		return new WorkflowStep(step.stage(), step.nameEn(), step.nameAr(), step.approverType(), step.managerLevel(),
				step.roleCode(), step.userId(), step.conditionField(), step.conditionOperator(), step.conditionValue(),
				step.ifUnresolved(), step.fallbackRoleCode());
	}

	private void requireRole(String code) {
		if (code == null || users.findRole(code).isEmpty()) {
			throw ApiException.unprocessable("workflow.role-not-found", code);
		}
	}

	record WorkflowRequest(@NotBlank @Pattern(regexp = "[A-Z0-9_]{2,30}", message = "{validation.code}") String code,
			@NotBlank @Size(max = 100) String nameEn, @NotBlank @Size(max = 100) String nameAr,
			@Size(max = 500) String description, boolean active,
			@NotEmpty @Size(max = 20) List<@Valid @NotNull StepRequest> steps) {
	}

	/**
	 * @param managerLevel for {@code MANAGER_LEVEL}: 2 = the manager's manager
	 * @param conditionField numeric attribute of the request ({@code days} for leave, a number field of a request
	 * type); all three condition fields or none
	 */
	record StepRequest(@Min(1) @Max(20) int stage, @NotBlank @Size(max = 100) String nameEn,
			@NotBlank @Size(max = 100) String nameAr, @NotNull ApproverType approverType,
			@Min(1) @Max(10) Integer managerLevel, String roleCode, Long userId,
			@Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_]{0,39}") String conditionField,
			ConditionOperator conditionOperator, BigDecimal conditionValue, @NotNull UnresolvedPolicy ifUnresolved,
			String fallbackRoleCode) {
	}

}
