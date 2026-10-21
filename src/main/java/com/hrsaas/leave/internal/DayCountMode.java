package com.hrsaas.leave.internal;

enum DayCountMode {

	/** Weekly rest days and public holidays inside the leave are not counted (annual, casual). */
	WORKING_DAYS,

	/** Every day counts (sick, maternity, Hajj). */
	CALENDAR_DAYS

}
