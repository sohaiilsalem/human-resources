package com.hrsaas.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Attendance marks the days as leave; payroll deducts unpaid days. */
public record LeaveApproved(UUID tenantId, UUID requestId, UUID employeeId, String leaveTypeCode,
		LeaveCategory category, boolean paid, LocalDate startDate, LocalDate endDate, boolean halfDay,
		BigDecimal days) {

}
