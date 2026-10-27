package com.hrsaas.leave;

import java.time.LocalDate;

/** An approved leave was withdrawn; consumers undo what they did for {@link LeaveApproved}. */
public record LeaveCancelled(Long tenantId, Long requestId, Long employeeId, LocalDate startDate,
		LocalDate endDate) {

}
