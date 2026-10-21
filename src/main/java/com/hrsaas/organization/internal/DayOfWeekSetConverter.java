package com.hrsaas.organization.internal;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** {@code FRIDAY,SATURDAY} ↔ {@code Set<DayOfWeek>}. */
@Converter
class DayOfWeekSetConverter implements AttributeConverter<Set<DayOfWeek>, String> {

	@Override
	public String convertToDatabaseColumn(Set<DayOfWeek> days) {
		return days.stream().sorted().map(DayOfWeek::name).collect(Collectors.joining(","));
	}

	@Override
	public Set<DayOfWeek> convertToEntityAttribute(String value) {
		Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
		if (value != null && !value.isBlank()) {
			Arrays.stream(value.split(",")).map(String::strip).map(DayOfWeek::valueOf).forEach(days::add);
		}
		return days;
	}

}
