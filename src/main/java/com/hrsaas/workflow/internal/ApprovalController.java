package com.hrsaas.workflow.internal;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.identity.Permission;
import com.hrsaas.identity.UserAccounts;
import com.hrsaas.shared.web.ApiException;
import com.hrsaas.workflow.ApprovalStatus;
import com.hrsaas.workflow.ApprovalView;
import com.hrsaas.workflow.Decision;

/**
 * Approvers' side, for every kind of request: the inbox, a request's approval timeline, and decisions. Who may act is
 * decided by the workflow rules, not by an endpoint permission.
 */
@RestController
@RequestMapping("/api/v1/approvals")
class ApprovalController {

	/** Placeholder for users without roles, so the inbox query never gets an empty IN list. */
	private static final Set<String> NO_ROLES = Set.of("-");

	private final ApprovalEngine engine;

	private final ApprovalInstanceRepository instances;

	private final ApprovalStepRepository steps;

	private final UserAccounts users;

	private final EmployeeDirectory employees;

	ApprovalController(ApprovalEngine engine, ApprovalInstanceRepository instances, ApprovalStepRepository steps,
			UserAccounts users, EmployeeDirectory employees) {
		this.engine = engine;
		this.instances = instances;
		this.steps = steps;
		this.users = users;
		this.employees = employees;
	}

	/** What the caller can approve or reject right now, oldest first. */
	@GetMapping("/inbox")
	@Transactional(readOnly = true)
	List<InboxItem> inbox(JwtAuthenticationToken auth) {
		Long userId = userId(auth);
		Set<String> roles = users.roleCodesOf(userId);
		Long ownEmployee = users.employeeIdOf(userId).orElse(-1L);
		List<ApprovalStep> pending = steps.inbox(userId, roles.isEmpty() ? NO_ROLES : roles, ownEmployee);
		Map<Long, EmployeeInfo> byId = employees.findAllById(
				pending.stream().map(step -> step.getInstance().getEmployeeId()).collect(Collectors.toSet()));
		return pending.stream().map(step -> {
			ApprovalInstance instance = step.getInstance();
			List<Integer> stages = instance.getSteps().stream().map(ApprovalStep::getStage).distinct().sorted().toList();
			EmployeeInfo employee = byId.get(instance.getEmployeeId());
			return new InboxItem(instance.getId(), instance.getSubjectType(), instance.getSubjectId(),
					instance.getEmployeeId(), employee != null ? employee.code() : null,
					employee != null ? employee.fullNameEn() : null, employee != null ? employee.fullNameAr() : null,
					instance.getSummaryEn(), instance.getSummaryAr(), step.getId(), step.getNameEn(), step.getNameAr(),
					stages.indexOf(step.getStage()) + 1, stages.size(), instance.getCreatedAt(), step.getUpdatedAt());
		}).toList();
	}

	/** All approvals, for workflow administrators. */
	@GetMapping
	@PreAuthorize("hasAuthority('workflow:manage')")
	@Transactional(readOnly = true)
	PagedModel<ApprovalView> search(@RequestParam(required = false) ApprovalStatus status,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return new PagedModel<>(instances.search(status, pageable).map(engine::view));
	}

	@GetMapping("/{id}")
	@Transactional(readOnly = true)
	ApprovalView get(JwtAuthenticationToken auth, @PathVariable Long id) {
		ApprovalInstance instance = instances.findById(id).orElseThrow(() -> ApiException.notFound("approval.not-found"));
		Long userId = userId(auth);
		if (!engine.isParticipant(instance, userId)
				&& !users.permissionsOf(userId).contains(Permission.WORKFLOW_MANAGE)) {
			throw ApiException.notFound("approval.not-found");
		}
		return engine.view(instance);
	}

	@PostMapping("/{id}/approve")
	ApprovalView approve(JwtAuthenticationToken auth, @PathVariable Long id,
			@Valid @RequestBody(required = false) Note note) {
		return engine.decide(id, userId(auth), Decision.APPROVE, note != null ? note.note() : null);
	}

	/** A reason is required: it is what the employee sees. */
	@PostMapping("/{id}/reject")
	ApprovalView reject(JwtAuthenticationToken auth, @PathVariable Long id,
			@Valid @RequestBody(required = false) Note note) {
		return engine.decide(id, userId(auth), Decision.REJECT, note != null ? note.note() : null);
	}

	@PostMapping("/{id}/steps/{stepId}/reassign")
	@PreAuthorize("hasAuthority('workflow:manage')")
	ApprovalView reassign(JwtAuthenticationToken auth, @PathVariable Long id, @PathVariable Long stepId,
			@Valid @RequestBody Reassignment request) {
		return engine.reassign(id, stepId, userId(auth), request.userId(), request.note().strip());
	}

	private static Long userId(JwtAuthenticationToken auth) {
		return Long.valueOf(auth.getToken().getSubject());
	}

	record Note(@Size(max = 500) String note) {
	}

	record Reassignment(@NotNull Long userId, @NotBlank @Size(max = 500) String note) {
	}

	record InboxItem(Long approvalId, String subjectType, Long subjectId, Long employeeId, String employeeCode,
			String employeeNameEn, String employeeNameAr, String summaryEn, String summaryAr, Long stepId,
			String stepNameEn, String stepNameAr, int stageNumber, int stageCount, Instant submittedAt,
			Instant waitingSince) {
	}

}
