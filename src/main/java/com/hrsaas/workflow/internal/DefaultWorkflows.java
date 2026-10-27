package com.hrsaas.workflow.internal;

import static com.hrsaas.workflow.internal.UnresolvedPolicy.ESCALATE;
import static com.hrsaas.workflow.internal.UnresolvedPolicy.FALLBACK;
import static com.hrsaas.workflow.internal.UnresolvedPolicy.SKIP;

import java.math.BigDecimal;
import java.util.List;

/**
 * Workflows every company starts with. Codes are referenced by the default leave and request types; companies on a
 * plan with approval workflows can edit them and add their own.
 */
final class DefaultWorkflows {

	static final String MANAGER = "MANAGER";

	private static final String HR = "HR_ADMIN";

	private static final String PAYROLL = "PAYROLL_ADMIN";

	private static final String OWNER = "OWNER";

	private DefaultWorkflows() {
	}

	static List<ApprovalWorkflow> create() {
		ApprovalWorkflow manager = new ApprovalWorkflow(MANAGER, "Direct manager", "المدير المباشر",
				"The employee's manager decides. If there is none, the next manager up, then HR.",
				List.of(WorkflowStep.directManager(1, ESCALATE, HR)));
		manager.markDefault(true);
		return List.of(manager,
				new ApprovalWorkflow("MANAGER_HR", "Direct manager, then HR", "المدير المباشر ثم الموارد البشرية",
						"The manager approves first, then HR.",
						List.of(WorkflowStep.directManager(1, ESCALATE, HR), hr(2))),
				new ApprovalWorkflow("MANAGERS_HR", "Two management levels, then HR",
						"مستويان من الإدارة ثم الموارد البشرية",
						"The manager, the manager's manager (skipped if there is none), then HR.",
						List.of(WorkflowStep.directManager(1, ESCALATE, HR), secondLevel(2), hr(3))),
				new ApprovalWorkflow("FINANCE", "Manager, then HR and payroll", "المدير ثم الموارد البشرية والرواتب",
						"The manager; the manager's manager for amounts above 20,000; then HR and payroll together.",
						List.of(WorkflowStep.directManager(1, ESCALATE, HR),
								secondLevel(2).when("amount", ConditionOperator.GT, new BigDecimal("20000")), hr(3),
								WorkflowStep.role(3, "Payroll", "الرواتب", PAYROLL, FALLBACK, HR))),
				new ApprovalWorkflow("HR_OWNER", "HR, then the owner", "الموارد البشرية ثم المالك",
						"For decisions such as terminations: HR first, then the company owner.",
						List.of(hr(1), WorkflowStep.role(2, "Company owner", "مالك الشركة", OWNER, FALLBACK, null))));
	}

	private static WorkflowStep hr(int stage) {
		return WorkflowStep.role(stage, "HR", "الموارد البشرية", HR, FALLBACK, OWNER);
	}

	private static WorkflowStep secondLevel(int stage) {
		return new WorkflowStep(stage, "Second-level manager", "مدير المستوى الثاني", ApproverType.MANAGER_LEVEL, 2,
				null, null, null, null, null, SKIP, null);
	}

}
