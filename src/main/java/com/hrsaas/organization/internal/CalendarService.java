package com.hrsaas.organization.internal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.organization.WorkCalendar;
import com.hrsaas.platform.TenantRegistered;
import com.hrsaas.shared.web.ApiException;

@Service
@Transactional(readOnly = true)
class CalendarService implements WorkCalendar {

	private final CalendarSettingsRepository settings;

	private final PublicHolidayRepository holidays;

	CalendarService(CalendarSettingsRepository settings, PublicHolidayRepository holidays) {
		this.settings = settings;
		this.holidays = holidays;
	}

	@EventListener
	@Transactional
	void createDefaultCalendar(TenantRegistered event) {
		settings.save(new CalendarSettings());
	}

	@Override
	public Period between(LocalDate from, LocalDate to) {
		Set<LocalDate> days = new HashSet<>();
		for (PublicHoliday holiday : holidays.findOverlapping(from, to)) {
			holiday.getStartDate().datesUntil(holiday.getEndDate().plusDays(1)).forEach(days::add);
		}
		return new CalendarPeriod(restDays(), Set.copyOf(days));
	}

	Set<DayOfWeek> restDays() {
		return settings.findCurrent().map(CalendarSettings::getRestDays).orElse(CalendarSettings.DEFAULT_REST_DAYS);
	}

	@Transactional
	Set<DayOfWeek> updateRestDays(Set<DayOfWeek> restDays) {
		if (restDays.size() > 3) {
			throw ApiException.unprocessable("organization.too-many-rest-days");
		}
		CalendarSettings current = settings.findCurrent().orElseGet(() -> settings.save(new CalendarSettings()));
		current.setRestDays(restDays);
		return current.getRestDays();
	}

	List<PublicHoliday> holidays(LocalDate from, LocalDate to) {
		return holidays.findOverlapping(from, to);
	}

	@Transactional
	PublicHoliday addHoliday(LocalDate start, LocalDate end, String nameEn, String nameAr) {
		if (end.isBefore(start)) {
			throw ApiException.unprocessable("organization.holiday-invalid-range");
		}
		return holidays.save(new PublicHoliday(start, end, nameEn.strip(), nameAr.strip()));
	}

	@Transactional
	void deleteHoliday(UUID id) {
		PublicHoliday holiday = holidays.findById(id)
			.orElseThrow(() -> ApiException.notFound("organization.holiday-not-found"));
		holidays.delete(holiday);
	}

	record CalendarPeriod(Set<DayOfWeek> restDays, Set<LocalDate> holidays) implements Period {

		@Override
		public boolean isWorkingDay(LocalDate date) {
			return !restDays.contains(date.getDayOfWeek()) && !holidays.contains(date);
		}

		@Override
		public int workingDays(LocalDate from, LocalDate to) {
			return (int) from.datesUntil(to.plusDays(1)).filter(this::isWorkingDay).count();
		}

	}

}
