package com.hrsaas.workflow;

/** {@code IN_PROGRESS → APPROVED | REJECTED}; {@code IN_PROGRESS | APPROVED → CANCELLED}. */
public enum ApprovalStatus {

	IN_PROGRESS, APPROVED, REJECTED, CANCELLED

}
