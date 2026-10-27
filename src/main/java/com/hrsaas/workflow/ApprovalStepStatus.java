package com.hrsaas.workflow;

/**
 * {@code WAITING} (a later stage) {@code → PENDING} (its stage is active, an approver is assigned)
 * {@code → APPROVED | REJECTED}. {@code SKIPPED} when no approver exists and the step allows skipping;
 * {@code CANCELLED} when the approval ends before the step is decided.
 */
public enum ApprovalStepStatus {

	WAITING, PENDING, APPROVED, REJECTED, SKIPPED, CANCELLED

}
