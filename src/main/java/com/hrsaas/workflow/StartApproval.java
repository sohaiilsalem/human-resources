package com.hrsaas.workflow;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @param workflowCode workflow to use; null for the tenant default
 * @param summaryEn one-line description shown in approvers' inboxes, e.g. "Annual leave · 3 days"
 * @param attributes numeric facts that step conditions can test, e.g. {@code days} or {@code amount}
 */
public record StartApproval(String subjectType, Long subjectId, Long employeeId, String workflowCode, Long requestedBy,
		String summaryEn, String summaryAr, Map<String, BigDecimal> attributes) {

	public StartApproval {
		attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
	}

}
