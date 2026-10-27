package com.hrsaas.workflow.internal;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.identity.Permission;
import com.hrsaas.identity.RoleInfo;
import com.hrsaas.identity.UserAccounts;
import com.hrsaas.shared.tenant.TenantContext;
import com.hrsaas.shared.web.ApiException;
import com.hrsaas.workflow.ApprovalActionType;
import com.hrsaas.workflow.ApprovalCompleted;
import com.hrsaas.workflow.ApprovalStatus;
import com.hrsaas.workflow.ApprovalStepStatus;
import com.hrsaas.workflow.ApprovalView;
import com.hrsaas.workflow.Approvals;
import com.hrsaas.workflow.Decision;
import com.hrsaas.workflow.StartApproval;

/**
 * Runs approvals. Stages run in order; all steps of a stage must approve (or be skipped) before the next stage is
 * activated, and one rejection rejects the whole request. See docs/WORKFLOWS.md for the full rules.
 */
@Service
@Transactional
class ApprovalEngine implements Approvals {

	private final ApprovalWorkflowRepository workflows;

	private final ApprovalInstanceRepository instances;

	private final ApprovalStepRepository steps;

	private final ApprovalActionRepository actions;

	private final ApproverResolver resolver;

	private final UserAccounts users;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	ApprovalEngine(ApprovalWorkflowRepository workflows, ApprovalInstanceRepository instances,
			ApprovalStepRepository steps, ApprovalActionRepository actions, ApproverResolver resolver, UserAccounts users,
			ApplicationEventPublisher events, Clock clock) {
		this.workflows = workflows;
		this.instances = instances;
		this.steps = steps;
		this.actions = actions;
		this.resolver = resolver;
		this.users = users;
		this.events = events;
		this.clock = clock;
	}

	// ------------------------------------------------------------------ Approvals

	@Override
	public ApprovalView start(StartApproval command) {
		if (instances.existsBySubjectTypeAndSubjectId(command.subjectType(), command.subjectId())) {
			throw ApiException.conflict("approval.already-started");
		}
		ApprovalWorkflow workflow = command.workflowCode() == null ? null
				: workflows.findByCode(command.workflowCode()).filter(ApprovalWorkflow::isActive).orElse(null);
		String note = null;
		if (workflow == null) {
			workflow = workflows.findByDefaultWorkflowTrue()
				.orElseThrow(() -> new IllegalStateException("Tenant has no default approval workflow"));
			if (command.workflowCode() != null) {
				note = "Workflow " + command.workflowCode() + " is not available; used the default " + workflow.getCode();
			}
		}
		ApprovalInstance instance = new ApprovalInstance(command, workflow.getCode());
		workflow.getSteps().stream().filter(step -> step.appliesTo(command.attributes())).forEach(instance::addStep);
		instances.save(instance);
		record(instance, null, ApprovalActionType.SUBMITTED, command.requestedBy(), note);
		advance(instance, command.requestedBy(), null);
		return view(instance);
	}

	@Override
	public ApprovalView decide(String subjectType, Long subjectId, Long actorUserId, Decision decision, String note) {
		return decide(lockedBySubject(subjectType, subjectId), actorUserId, decision, note);
	}

	@Override
	public void cancel(String subjectType, Long subjectId, Long actorUserId, String note) {
		instances.findBySubjectTypeAndSubjectId(subjectType, subjectId)
			.flatMap(found -> instances.lockById(found.getId()))
			.filter(instance -> instance.getStatus() == ApprovalStatus.IN_PROGRESS
					|| instance.getStatus() == ApprovalStatus.APPROVED)
			.ifPresent(instance -> {
				instance.complete(ApprovalStatus.CANCELLED, clock.instant());
				record(instance, null, ApprovalActionType.CANCELLED, actorUserId, note);
			});
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ApprovalView> find(String subjectType, Long subjectId) {
		return instances.findBySubjectTypeAndSubjectId(subjectType, subjectId).map(this::view);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isParticipant(String subjectType, Long subjectId, Long userId) {
		return instances.findBySubjectTypeAndSubjectId(subjectType, subjectId)
			.filter(instance -> isParticipant(instance, userId))
			.isPresent();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean workflowExists(String code) {
		return workflows.findByCode(code).filter(ApprovalWorkflow::isActive).isPresent();
	}

	// ------------------------------------------------------------------ used by the approvals API

	ApprovalView decide(Long instanceId, Long actorUserId, Decision decision, String note) {
		return decide(instances.lockById(instanceId).orElseThrow(() -> ApiException.notFound("approval.not-found")),
				actorUserId, decision, note);
	}

	/** Hands a pending step to another user, e.g. when the assigned approver is away. */
	ApprovalView reassign(Long instanceId, Long stepId, Long actorUserId, Long toUserId, String note) {
		ApprovalInstance instance = instances.lockById(instanceId)
			.orElseThrow(() -> ApiException.notFound("approval.not-found"));
		ApprovalStep step = instance.getSteps()
			.stream()
			.filter(candidate -> candidate.getId().equals(stepId))
			.findFirst()
			.orElseThrow(() -> ApiException.notFound("approval.step-not-found"));
		if (!instance.isInProgress() || !step.isPending()) {
			throw ApiException.conflict("approval.step-not-pending");
		}
		if (!users.isActive(toUserId)) {
			throw ApiException.unprocessable("approval.user-not-active");
		}
		if (users.employeeIdOf(toUserId).filter(instance.getEmployeeId()::equals).isPresent()) {
			throw ApiException.unprocessable("approval.cannot-assign-to-requester");
		}
		step.assignUser(toUserId);
		record(instance, step, ApprovalActionType.REASSIGNED, actorUserId, note);
		return view(instance);
	}

	boolean isParticipant(ApprovalInstance instance, Long userId) {
		if (userId.equals(instance.getRequestedBy())
				|| users.employeeIdOf(userId).filter(instance.getEmployeeId()::equals).isPresent()) {
			return true;
		}
		Set<String> roles = users.roleCodesOf(userId);
		return instance.getSteps()
			.stream()
			.anyMatch(step -> userId.equals(step.getAssigneeUserId()) || userId.equals(step.getDecidedBy())
					|| (step.getStatus() != ApprovalStepStatus.WAITING && step.getAssigneeUserId() == null
							&& roles.contains(step.getAssigneeRoleCode())));
	}

	// ------------------------------------------------------------------ the rules

	private ApprovalView decide(ApprovalInstance instance, Long actorUserId, Decision decision, String note) {
		if (!instance.isInProgress()) {
			throw ApiException.conflict("approval.not-pending");
		}
		if (users.employeeIdOf(actorUserId).filter(instance.getEmployeeId()::equals).isPresent()) {
			throw ApiException.forbidden("approval.cannot-decide-own");
		}
		String cleanNote = blankToNull(note);
		if (decision == Decision.REJECT && cleanNote == null) {
			throw ApiException.unprocessable("approval.reason-required");
		}
		ApprovalStepStatus outcome = decision == Decision.APPROVE ? ApprovalStepStatus.APPROVED
				: ApprovalStepStatus.REJECTED;
		Set<String> roles = users.roleCodesOf(actorUserId);
		Optional<ApprovalStep> own = instance.currentSteps()
			.stream()
			.filter(step -> step.isPending() && canDecide(step, actorUserId, roles))
			.findFirst();
		if (own.isPresent()) {
			own.get().decide(outcome, actorUserId, clock.instant(), cleanNote);
			record(instance, own.get(), decision == Decision.APPROVE ? ApprovalActionType.APPROVED
					: ApprovalActionType.REJECTED, actorUserId, cleanNote);
		}
		else if (users.permissionsOf(actorUserId).contains(Permission.WORKFLOW_MANAGE)) {
			// administrative override of the whole current stage; always visible in the history
			for (ApprovalStep step : instance.currentSteps()) {
				if (step.isPending()) {
					step.decide(outcome, actorUserId, clock.instant(), cleanNote);
					record(instance, step, decision == Decision.APPROVE ? ApprovalActionType.OVERRIDE_APPROVED
							: ApprovalActionType.OVERRIDE_REJECTED, actorUserId, cleanNote);
				}
			}
		}
		else {
			throw ApiException.forbidden("approval.not-eligible");
		}
		if (decision == Decision.REJECT) {
			complete(instance, ApprovalStatus.REJECTED, actorUserId, cleanNote);
		}
		else if (instance.currentStageDone()) {
			advance(instance, actorUserId, cleanNote);
		}
		return view(instance);
	}

	private static boolean canDecide(ApprovalStep step, Long userId, Set<String> roles) {
		return step.getAssigneeUserId() != null ? step.getAssigneeUserId().equals(userId)
				: roles.contains(step.getAssigneeRoleCode());
	}

	/**
	 * Activates the next stage that has someone to decide, skipping stages whose steps were all skipped. When no stage
	 * is left the request is approved, unless nobody approved anything at all (every step skipped or filtered out by
	 * its condition): then a final step for the owner role is added, so nothing is approved by default.
	 */
	private void advance(ApprovalInstance instance, Long lastActor, String lastNote) {
		while (true) {
			OptionalInt next = instance.nextStage();
			if (next.isEmpty()) {
				if (!instance.anyApproved()) {
					int stage = instance.getCurrentStage() == null ? 1 : instance.getCurrentStage() + 1;
					steps.save(instance.addStep(WorkflowStep.role(stage, "Final approval", "الموافقة النهائية",
							WorkflowStep.DEFAULT_FALLBACK_ROLE, UnresolvedPolicy.FALLBACK, null)));
					continue;
				}
				complete(instance, ApprovalStatus.APPROVED, lastActor, lastNote);
				return;
			}
			instance.activate(next.getAsInt());
			boolean waitingForSomeone = false;
			for (ApprovalStep step : instance.currentSteps()) {
				ApproverResolver.Resolution resolution = resolver.resolve(step, instance.getEmployeeId());
				record(instance, step, resolution.action(), null, resolution.note());
				waitingForSomeone |= step.isPending();
			}
			if (waitingForSomeone) {
				return;
			}
		}
	}

	private void complete(ApprovalInstance instance, ApprovalStatus outcome, Long decidedBy, String note) {
		instance.complete(outcome, clock.instant());
		record(instance, null, ApprovalActionType.COMPLETED, null, outcome.name());
		events.publishEvent(new ApprovalCompleted(TenantContext.require(), instance.getId(), instance.getSubjectType(),
				instance.getSubjectId(), instance.getEmployeeId(), outcome, decidedBy, note));
	}

	private void record(ApprovalInstance instance, ApprovalStep step, ApprovalActionType type, Long actor,
			String note) {
		actions.save(new ApprovalAction(instance.getId(), step != null ? step.getId() : null, type, actor, note));
	}

	private ApprovalInstance lockedBySubject(String subjectType, Long subjectId) {
		return instances.findBySubjectTypeAndSubjectId(subjectType, subjectId)
			.flatMap(found -> instances.lockById(found.getId()))
			.orElseThrow(() -> ApiException.notFound("approval.not-found"));
	}

	// ------------------------------------------------------------------ views

	ApprovalView view(ApprovalInstance instance) {
		List<ApprovalStep> steps = instance.getSteps();
		List<ApprovalAction> history = actions.findByInstanceIdOrderById(instance.getId());
		Set<Long> userIds = new HashSet<>();
		Set<String> roleCodes = new HashSet<>();
		steps.forEach(step -> {
			Stream.of(step.getAssigneeUserId(), step.getDecidedBy()).filter(id -> id != null).forEach(userIds::add);
			if (step.getAssigneeRoleCode() != null) {
				roleCodes.add(step.getAssigneeRoleCode());
			}
		});
		history.stream().map(ApprovalAction::getActorUserId).filter(id -> id != null).forEach(userIds::add);
		Map<Long, String> names = users.fullNamesOf(userIds);
		Map<String, RoleInfo> roles = users.findRoles(roleCodes);

		List<Integer> stages = steps.stream().map(ApprovalStep::getStage).distinct().sorted().toList();
		List<ApprovalView.Step> stepViews = new ArrayList<>();
		for (ApprovalStep step : steps) {
			String nameEn = null;
			String nameAr = null;
			if (step.getAssigneeUserId() != null) {
				nameEn = nameAr = names.get(step.getAssigneeUserId());
			}
			else if (step.getAssigneeRoleCode() != null) {
				RoleInfo role = roles.get(step.getAssigneeRoleCode());
				nameEn = role != null ? role.nameEn() : step.getAssigneeRoleCode();
				nameAr = role != null ? role.nameAr() : step.getAssigneeRoleCode();
			}
			stepViews.add(new ApprovalView.Step(step.getId(), stages.indexOf(step.getStage()) + 1, step.getNameEn(),
					step.getNameAr(), step.getStatus(), step.getAssigneeUserId(), step.getAssigneeRoleCode(), nameEn,
					nameAr, step.getDecidedBy(), names.get(step.getDecidedBy()), step.getDecidedAt(), step.getNote()));
		}
		List<ApprovalView.Action> actionViews = history.stream()
			.map(action -> new ApprovalView.Action(action.getAction(), action.getStepId(), action.getActorUserId(),
					names.get(action.getActorUserId()), action.getNote(), action.getCreatedAt()))
			.toList();
		Integer current = instance.isInProgress() && instance.getCurrentStage() != null
				? stages.indexOf(instance.getCurrentStage()) + 1 : null;
		return new ApprovalView(instance.getId(), instance.getSubjectType(), instance.getSubjectId(),
				instance.getEmployeeId(), instance.getWorkflowCode(), instance.getSummaryEn(), instance.getSummaryAr(),
				instance.getStatus(), current, stages.size(), stepViews, actionViews, instance.getCreatedAt(),
				instance.getCompletedAt());
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
