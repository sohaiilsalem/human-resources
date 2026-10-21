package com.hrsaas.leave.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.hrsaas.organization.WorkCalendar;

class LeaveDaysTest {

	// Sun 2026-10-04 … Sat 2026-10-10; Tuesday 6 October (Armed Forces Day) is a holiday
	private final WorkCalendar.Period calendar = new WorkCalendar.Period() {

		@Override
		public boolean isWorkingDay(LocalDate date) {
			return switch (date.getDayOfWeek()) {
				case FRIDAY, SATURDAY -> false;
				default -> !holidays().contains(date);
			};
		}

		@Override
		public int workingDays(LocalDate from, LocalDate to) {
			return (int) from.datesUntil(to.plusDays(1)).filter(this::isWorkingDay).count();
		}

		@Override
		public Set<LocalDate> holidays() {
			return Set.of(LocalDate.parse("2026-10-06"));
		}

	};

	@Test
	void workingDaysSkipRestDaysAndHolidays() {
		assertThat(count(DayCountMode.WORKING_DAYS, "2026-10-04", "2026-10-11", false)).isEqualByComparingTo("5");
	}

	@Test
	void calendarDaysCountEveryDay() {
		assertThat(count(DayCountMode.CALENDAR_DAYS, "2026-10-04", "2026-10-11", false)).isEqualByComparingTo("8");
	}

	@Test
	void halfDayOnAWorkingDayAndNothingOnAHoliday() {
		assertThat(count(DayCountMode.WORKING_DAYS, "2026-10-05", "2026-10-05", true)).isEqualByComparingTo("0.5");
		assertThat(count(DayCountMode.WORKING_DAYS, "2026-10-06", "2026-10-06", true)).isEqualByComparingTo("0");
		assertThat(count(DayCountMode.WORKING_DAYS, "2026-10-09", "2026-10-10", false)).isEqualByComparingTo("0");
	}

	private BigDecimal count(DayCountMode mode, String start, String end, boolean halfDay) {
		return LeaveDays.count(mode, calendar, LocalDate.parse(start), LocalDate.parse(end), halfDay);
	}

}
