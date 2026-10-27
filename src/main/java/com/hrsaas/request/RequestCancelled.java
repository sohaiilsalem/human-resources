package com.hrsaas.request;

/** An approved request was withdrawn; consumers undo what they did for {@link RequestApproved}. */
public record RequestCancelled(Long tenantId, Long requestId, Long employeeId, String requestType) {

}
