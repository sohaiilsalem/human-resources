package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.hrsaas.organization.WorkCalendar;

/** How many days a leave over {@code [start, end]} consumes. */
final class LeaveDays {

	static final BigDecimal HALF = new BigDecimal("0.5");

	private LeaveDays() {
	}

	static BigDecimal count(DayCountMode mode, WorkCalendar.Period calendar, LocalDate start, LocalDate end,
			boolean halfDay) {
		long days = switch (mode) {
			case WORKING_DAYS -> calendar.workingDays(start, end);
			case CALENDAR_DAYS -> ChronoUnit.DAYS.between(start, end) + 1;
		};
		if (halfDay) {
			return days == 0 ? BigDecimal.ZERO : HALF;
		}
		return BigDecimal.valueOf(days);
	}

}
