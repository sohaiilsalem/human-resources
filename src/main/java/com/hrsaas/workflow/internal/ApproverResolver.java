package com.hrsaas.workflow.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.identity.UserAccounts;
import com.hrsaas.workflow.ApprovalActionType;

/**
 * Assigns an approver to a step when its stage becomes active, applying the step's {@link UnresolvedPolicy} when the
 * natural approver is missing. The employee the request is about is never a candidate.
 */
@Component
class ApproverResolver {

	/** Guards against cycles and absurdly deep hierarchies in the manager chain. */
	private static final int MAX_CHAIN = 20;

	private final EmployeeDirectory employees;

	private final UserAccounts users;

	ApproverResolver(EmployeeDirectory employees, UserAccounts users) {
		this.employees = employees;
		this.users = users;
	}

	record Resolution(ApprovalActionType action, String note) {
	}

	Resolution resolve(ApprovalStep step, Long employeeId) {
		Long requesterUser = users.activeUserIdOfEmployee(employeeId).orElse(null);
		return switch (step.getApproverType()) {
			case DIRECT_MANAGER -> manager(step, employeeId, 1);
			case MANAGER_LEVEL -> manager(step, employeeId, step.getManagerLevel());
			case ROLE -> {
				if (hasCandidates(step.getRoleCode(), requesterUser)) {
					step.assignRole(step.getRoleCode());
					yield new Resolution(ApprovalActionType.ASSIGNED, null);
				}
				yield unresolved(step, "Nobody else holds role " + step.getRoleCode());
			}
			case USER -> {
				if (users.isActive(step.getUserId()) && !step.getUserId().equals(requesterUser)) {
					step.assignUser(step.getUserId());
					yield new Resolution(ApprovalActionType.ASSIGNED, null);
				}
				yield unresolved(step, "The named approver is not available");
			}
		};
	}

	private Resolution manager(ApprovalStep step, Long employeeId, int level) {
		List<Long> chain = managerChain(employeeId);
		Optional<Long> approver = chain.size() >= level ? usableManager(chain.get(level - 1)) : Optional.empty();
		if (approver.isPresent()) {
			step.assignUser(approver.get());
			return new Resolution(ApprovalActionType.ASSIGNED, null);
		}
		String missing = level == 1 ? "No active direct manager" : "No active manager at level " + level;
		if (step.getIfUnresolved() == UnresolvedPolicy.ESCALATE) {
			for (int i = level; i < chain.size(); i++) {
				Optional<Long> higher = usableManager(chain.get(i));
				if (higher.isPresent()) {
					step.assignUser(higher.get());
					return new Resolution(ApprovalActionType.ESCALATED,
							missing + "; escalated to the manager at level " + (i + 1));
				}
			}
		}
		return unresolved(step, missing);
	}

	/**
	 * SKIP skips. ESCALATE (once the reporting line is exhausted) and FALLBACK assign the fallback role, falling back
	 * once more to the owner role if nobody else holds it; such a step is never skipped, so a request cannot be
	 * approved by nobody.
	 */
	private Resolution unresolved(ApprovalStep step, String reason) {
		if (step.getIfUnresolved() == UnresolvedPolicy.SKIP) {
			step.skip();
			return new Resolution(ApprovalActionType.SKIPPED, reason);
		}
		Long requesterUser = users.activeUserIdOfEmployee(step.getInstance().getEmployeeId()).orElse(null);
		String role = step.fallbackRole();
		if (!hasCandidates(role, requesterUser) && !role.equals(WorkflowStep.DEFAULT_FALLBACK_ROLE)) {
			role = WorkflowStep.DEFAULT_FALLBACK_ROLE;
		}
		step.assignRole(role);
		return new Resolution(ApprovalActionType.ESCALATED, reason + "; assigned to role " + role);
	}

	private boolean hasCandidates(String roleCode, Long excludedUser) {
		return users.activeUserIdsWithRole(roleCode).stream().anyMatch(id -> !id.equals(excludedUser));
	}

	/** Manager ids from the direct manager upwards. */
	private List<Long> managerChain(Long employeeId) {
		List<Long> chain = new ArrayList<>();
		Set<Long> seen = new HashSet<>(Set.of(employeeId));
		Long current = employees.findById(employeeId).map(EmployeeInfo::managerId).orElse(null);
		while (current != null && seen.add(current) && chain.size() < MAX_CHAIN) {
			chain.add(current);
			current = employees.findById(current).map(EmployeeInfo::managerId).orElse(null);
		}
		return chain;
	}

	/** A manager counts if they are still employed and have an active account to act with. */
	private Optional<Long> usableManager(Long managerEmployeeId) {
		return employees.findById(managerEmployeeId)
			.filter(EmployeeInfo::isActive)
			.flatMap(manager -> users.activeUserIdOfEmployee(manager.id()));
	}

}
