package com.hrsaas.workflow;

/** Entries of the audit trail. */
public enum ApprovalActionType {

	SUBMITTED, ASSIGNED, ESCALATED, SKIPPED, APPROVED, REJECTED, OVERRIDE_APPROVED, OVERRIDE_REJECTED, REASSIGNED,
	CANCELLED, COMPLETED

}
