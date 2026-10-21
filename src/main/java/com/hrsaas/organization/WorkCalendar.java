package com.hrsaas.organization;

import java.time.LocalDate;
import java.util.Set;

/** The current tenant's working calendar: weekly rest days and public holidays. */
public interface WorkCalendar {

	/** A snapshot of the calendar over {@code [from, to]}, so callers can test many days with one lookup. */
	Period between(LocalDate from, LocalDate to);

	interface Period {

		boolean isWorkingDay(LocalDate date);

		/** Working days in {@code [from, to]} inclusive. */
		int workingDays(LocalDate from, LocalDate to);

		Set<LocalDate> holidays();

	}

}
