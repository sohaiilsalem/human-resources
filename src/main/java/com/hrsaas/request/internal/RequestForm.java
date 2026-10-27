package com.hrsaas.request.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hrsaas.shared.web.ApiException;

/** Checks submitted values against a type's fields and normalizes them for storage. */
final class RequestForm {

	private RequestForm() {
	}

	/** Field definitions themselves: unique keys, options for SELECT, a sensible range. */
	static void checkDefinitions(List<FieldDefinition> fields) {
		Set<String> keys = new HashSet<>();
		for (FieldDefinition field : fields) {
			if (!keys.add(field.key())) {
				throw ApiException.unprocessable("request.duplicate-field", field.key());
			}
			if (field.type() == FieldType.SELECT && (field.options() == null || field.options().isEmpty())) {
				throw ApiException.unprocessable("request.select-needs-options", field.key());
			}
			if (field.min() != null && field.max() != null && field.min().compareTo(field.max()) > 0) {
				throw ApiException.unprocessable("request.invalid-range", field.key());
			}
		}
	}

	/**
	 * @return the values in field order: text, dates and times as strings, numbers as {@link BigDecimal}, booleans
	 * as {@link Boolean}; optional fields left empty are omitted
	 */
	static Map<String, Object> validate(List<FieldDefinition> fields, Map<String, Object> submitted) {
		Map<String, Object> input = submitted == null ? Map.of() : submitted;
		Set<String> known = new HashSet<>();
		Map<String, Object> values = new LinkedHashMap<>();
		for (FieldDefinition field : fields) {
			known.add(field.key());
			Object raw = input.get(field.key());
			if (raw == null || (raw instanceof String text && text.isBlank())) {
				if (field.required()) {
					throw ApiException.unprocessable("request.field-required", field.labelEn());
				}
				continue;
			}
			values.put(field.key(), convert(field, raw));
		}
		for (String key : input.keySet()) {
			if (!known.contains(key)) {
				throw ApiException.unprocessable("request.unknown-field", key);
			}
		}
		return values;
	}

	/** Numeric values, which workflow step conditions can test ({@code amount GT 20000}). */
	static Map<String, BigDecimal> attributes(List<FieldDefinition> fields, Map<String, Object> values) {
		Map<String, BigDecimal> attributes = new HashMap<>();
		for (FieldDefinition field : fields) {
			if (field.isNumeric() && values.get(field.key()) instanceof BigDecimal number) {
				attributes.put(field.key(), number);
			}
		}
		return attributes;
	}

	private static Object convert(FieldDefinition field, Object raw) {
		try {
			return switch (field.type()) {
				case TEXT -> text(field, raw, 200);
				case LONG_TEXT -> text(field, raw, 2000);
				case NUMBER, AMOUNT -> number(field, raw);
				case DATE -> LocalDate.parse(raw.toString()).toString();
				case TIME -> LocalTime.parse(raw.toString()).toString();
				case BOOLEAN -> {
					if (raw instanceof Boolean bool) {
						yield bool;
					}
					throw invalid(field);
				}
				case SELECT -> {
					if (field.options().contains(raw.toString())) {
						yield raw.toString();
					}
					throw invalid(field);
				}
			};
		}
		catch (DateTimeParseException | NumberFormatException ex) {
			throw invalid(field);
		}
	}

	private static String text(FieldDefinition field, Object raw, int maxLength) {
		String value = raw.toString().strip();
		if (value.length() > maxLength) {
			throw invalid(field);
		}
		return value;
	}

	private static BigDecimal number(FieldDefinition field, Object raw) {
		if (raw instanceof Boolean) {
			throw invalid(field);
		}
		BigDecimal value = new BigDecimal(raw.toString().strip());
		if (field.type() == FieldType.AMOUNT && value.stripTrailingZeros().scale() > 2) {
			throw invalid(field);
		}
		if ((field.min() != null && value.compareTo(field.min()) < 0)
				|| (field.max() != null && value.compareTo(field.max()) > 0)) {
			throw ApiException.unprocessable("request.field-out-of-range", field.labelEn(), plain(field.min()),
					plain(field.max()));
		}
		return value;
	}

	private static String plain(BigDecimal value) {
		return value == null ? "-" : value.stripTrailingZeros().toPlainString();
	}

	private static ApiException invalid(FieldDefinition field) {
		return ApiException.unprocessable("request.invalid-field", field.labelEn());
	}

}
