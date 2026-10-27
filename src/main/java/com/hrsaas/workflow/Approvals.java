package com.hrsaas.workflow;

import java.util.Optional;

/**
 * Approval engine used by modules that own something to approve (a leave request, an employee request, ...). The
 * owning module checks its own business rules; the engine decides <em>who</em> may approve, in which order, and keeps
 * the audit trail. The outcome is announced with {@link ApprovalCompleted}, published synchronously in the deciding
 * transaction, so a listener that throws rolls the decision back.
 *
 * <p>
 * A subject is identified by {@code (subjectType, subjectId)}, e.g. {@code ("LEAVE", 42)}.
 */
public interface Approvals {

	/**
	 * Starts the approval of a new subject with the named workflow, or the tenant's default workflow when the code is
	 * null, unknown or inactive.
	 */
	ApprovalView start(StartApproval command);

	/**
	 * Records the actor's decision on the step they may decide in the current stage, or, for holders of
	 * {@code workflow:manage}, an audited override of the whole current stage.
	 * @throws com.hrsaas.shared.web.ApiException if the actor is not allowed to decide now
	 */
	ApprovalView decide(String subjectType, Long subjectId, Long actorUserId, Decision decision, String note);

	/** Withdraws a running or approved subject; a no-op if it has no approval or is already closed. */
	void cancel(String subjectType, Long subjectId, Long actorUserId, String note);

	Optional<ApprovalView> find(String subjectType, Long subjectId);

	/** Whether the user takes part in the approval: requester, a current or past approver, or eligible for a step. */
	boolean isParticipant(String subjectType, Long subjectId, Long userId);

	/** Whether an active workflow with this code exists in the current tenant. */
	boolean workflowExists(String code);

}
