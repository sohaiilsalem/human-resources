package com.hrsaas.leave;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Attendance marks the days as leave; payroll deducts unpaid days. */
public record LeaveApproved(Long tenantId, Long requestId, Long employeeId, String leaveTypeCode,
		LeaveCategory category, boolean paid, LocalDate startDate, LocalDate endDate, boolean halfDay,
		BigDecimal days) {

}
