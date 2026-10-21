package com.hrsaas.organization.internal;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;

/** One row per tenant. Shifts and per-branch calendars come with the attendance module. */
@Entity
@Table(name = "work_calendars")
class CalendarSettings extends TenantEntity {

	/** Egyptian private sector default: Friday and Saturday. */
	static final Set<DayOfWeek> DEFAULT_REST_DAYS = EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

	@Convert(converter = DayOfWeekSetConverter.class)
	@Column(nullable = false, length = 70)
	private Set<DayOfWeek> restDays = EnumSet.copyOf(DEFAULT_REST_DAYS);

	CalendarSettings() {
	}

	Set<DayOfWeek> getRestDays() {
		return Set.copyOf(restDays);
	}

	void setRestDays(Set<DayOfWeek> restDays) {
		this.restDays = EnumSet.copyOf(restDays);
	}

}
