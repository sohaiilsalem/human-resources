package com.hrsaas.workflow;

/**
 * The final outcome of an approval, {@link ApprovalStatus#APPROVED} or {@link ApprovalStatus#REJECTED}. Published
 * synchronously inside the transaction of the last decision; the owning module applies the outcome to its subject.
 */
public record ApprovalCompleted(Long tenantId, Long approvalId, String subjectType, Long subjectId, Long employeeId,
		ApprovalStatus outcome, Long decidedBy, String note) {

}
