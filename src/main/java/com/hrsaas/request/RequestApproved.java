package com.hrsaas.request;

import java.util.Map;

/**
 * An employee request was fully approved. Consumers act on the types they understand, e.g. payroll on
 * {@code LOAN} / {@code SALARY_ADVANCE}, attendance on {@link RequestCategory#PERMISSION} types.
 * @param data the submitted form values by field key (strings, numbers and booleans)
 */
public record RequestApproved(Long tenantId, Long requestId, Long employeeId, String requestType,
		RequestCategory category, Map<String, Object> data) {

}
