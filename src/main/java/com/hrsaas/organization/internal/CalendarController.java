package com.hrsaas.organization.internal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.billing.Feature;
import com.hrsaas.billing.RequiresFeature;

@RestController
@RequestMapping("/api/v1/organization")
@RequiresFeature(Feature.CORE_HR)
class CalendarController {

	private final CalendarService calendar;

	CalendarController(CalendarService calendar) {
		this.calendar = calendar;
	}

	@GetMapping("/calendar")
	CalendarResponse calendar() {
		return new CalendarResponse(calendar.restDays());
	}

	@PutMapping("/calendar")
	@PreAuthorize("hasAuthority('organization:manage')")
	CalendarResponse updateCalendar(@Valid @RequestBody CalendarResponse request) {
		return new CalendarResponse(calendar.updateRestDays(request.restDays()));
	}

	@GetMapping("/holidays")
	List<HolidayResponse> holidays(@RequestParam int year) {
		return calendar.holidays(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
			.stream()
			.map(HolidayResponse::of)
			.toList();
	}

	@PostMapping("/holidays")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('organization:manage')")
	HolidayResponse addHoliday(@Valid @RequestBody HolidayRequest request) {
		LocalDate end = request.endDate() != null ? request.endDate() : request.startDate();
		return HolidayResponse.of(calendar.addHoliday(request.startDate(), end, request.nameEn(), request.nameAr()));
	}

	@DeleteMapping("/holidays/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('organization:manage')")
	void deleteHoliday(@PathVariable Long id) {
		calendar.deleteHoliday(id);
	}

	record CalendarResponse(@NotNull Set<DayOfWeek> restDays) {
	}

	record HolidayRequest(@NotNull LocalDate startDate, LocalDate endDate, @NotBlank @Size(max = 200) String nameEn,
			@NotBlank @Size(max = 200) String nameAr) {
	}

	record HolidayResponse(Long id, LocalDate startDate, LocalDate endDate, String nameEn, String nameAr) {

		static HolidayResponse of(PublicHoliday holiday) {
			return new HolidayResponse(holiday.getId(), holiday.getStartDate(), holiday.getEndDate(),
					holiday.getNameEn(), holiday.getNameAr());
		}

	}

}
