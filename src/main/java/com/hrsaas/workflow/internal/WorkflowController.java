package com.hrsaas.workflow.internal;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.billing.Feature;
import com.hrsaas.billing.RequiresFeature;
import com.hrsaas.workflow.internal.WorkflowAdminService.WorkflowRequest;

/**
 * Workflow configuration. Every plan runs the default workflows; changing them or adding new ones needs the
 * approval-workflows feature.
 */
@RestController
@RequestMapping("/api/v1/workflows")
class WorkflowController {

	private final WorkflowAdminService admin;

	WorkflowController(WorkflowAdminService admin) {
		this.admin = admin;
	}

	@GetMapping
	@PreAuthorize("hasAnyAuthority('workflow:manage', 'request:manage', 'leave:manage')")
	List<WorkflowView> list() {
		return admin.list().stream().map(WorkflowView::of).toList();
	}

	@GetMapping("/{code}")
	@PreAuthorize("hasAnyAuthority('workflow:manage', 'request:manage', 'leave:manage')")
	WorkflowView get(@PathVariable String code) {
		return WorkflowView.of(admin.get(code));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('workflow:manage')")
	@RequiresFeature(Feature.APPROVAL_WORKFLOWS)
	WorkflowView create(@Valid @RequestBody WorkflowRequest request) {
		return WorkflowView.of(admin.create(request));
	}

	@PutMapping("/{code}")
	@PreAuthorize("hasAuthority('workflow:manage')")
	@RequiresFeature(Feature.APPROVAL_WORKFLOWS)
	WorkflowView update(@PathVariable String code, @Valid @RequestBody WorkflowRequest request) {
		return WorkflowView.of(admin.update(code, request));
	}

	@PostMapping("/{code}/default")
	@PreAuthorize("hasAuthority('workflow:manage')")
	@RequiresFeature(Feature.APPROVAL_WORKFLOWS)
	WorkflowView makeDefault(@PathVariable String code) {
		return WorkflowView.of(admin.makeDefault(code));
	}

	record WorkflowView(Long id, String code, String nameEn, String nameAr, String description, boolean isDefault,
			boolean active, List<StepView> steps) {

		static WorkflowView of(ApprovalWorkflow workflow) {
			return new WorkflowView(workflow.getId(), workflow.getCode(), workflow.getNameEn(), workflow.getNameAr(),
					workflow.getDescription(), workflow.isDefault(), workflow.isActive(),
					workflow.getSteps().stream().map(StepView::of).toList());
		}

	}

	record StepView(int stage, String nameEn, String nameAr, ApproverType approverType, Integer managerLevel,
			String roleCode, Long userId, String conditionField, ConditionOperator conditionOperator,
			BigDecimal conditionValue, UnresolvedPolicy ifUnresolved, String fallbackRoleCode) {

		static StepView of(WorkflowStep step) {
			return new StepView(step.getStage(), step.getNameEn(), step.getNameAr(), step.getApproverType(),
					step.getManagerLevel(), step.getRoleCode(), step.getUserId(), step.getConditionField(),
					step.getConditionOperator(), step.getConditionValue(), step.getIfUnresolved(),
					step.getFallbackRoleCode());
		}

	}

}
