package com.hrsaas.leave;

import java.time.LocalDate;
import java.util.UUID;

/** An approved leave was withdrawn; consumers undo what they did for {@link LeaveApproved}. */
public record LeaveCancelled(UUID tenantId, UUID requestId, UUID employeeId, LocalDate startDate,
		LocalDate endDate) {

}
