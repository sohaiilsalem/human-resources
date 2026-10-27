package com.hrsaas.workflow;

import java.time.Instant;
import java.util.List;

/**
 * An approval as shown to the employee and the approvers. {@code currentStageNumber} counts stages from 1 in the
 * order they run ("step 2 of 3"), whatever stage numbers the workflow uses.
 */
public record ApprovalView(Long id, String subjectType, Long subjectId, Long employeeId, String workflowCode,
		String summaryEn, String summaryAr, ApprovalStatus status, Integer currentStageNumber, int stageCount,
		List<Step> steps, List<Action> history, Instant createdAt, Instant completedAt) {

	/** {@code assigneeUserId} is set for a person; for a role it is null and the names are the role's. */
	public record Step(Long id, int stageNumber, String nameEn, String nameAr, ApprovalStepStatus status,
			Long assigneeUserId, String assigneeRole, String assigneeNameEn, String assigneeNameAr, Long decidedBy,
			String decidedByName, Instant decidedAt, String note) {
	}

	public record Action(ApprovalActionType action, Long stepId, Long actorUserId, String actorName, String note,
			Instant at) {
	}

}
