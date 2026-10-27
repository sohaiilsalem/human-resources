package com.hrsaas.request.internal;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One field of a request type's form, stored in {@code request_types.fields} (JSON).
 * @param key name of the value in the submitted data; also usable in workflow step conditions when numeric
 * @param min for NUMBER / AMOUNT, the smallest allowed value
 * @param max for NUMBER / AMOUNT, the largest allowed value
 */
record FieldDefinition(@NotBlank @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_]{0,39}") String key,
		@NotBlank @Size(max = 100) String labelEn, @NotBlank @Size(max = 100) String labelAr, @NotNull FieldType type,
		boolean required, @Size(max = 50) List<@NotBlank @Size(max = 50) String> options, BigDecimal min,
		BigDecimal max) {

	static FieldDefinition of(String key, String labelEn, String labelAr, FieldType type, boolean required) {
		return new FieldDefinition(key, labelEn, labelAr, type, required, null, null, null);
	}

	FieldDefinition withRange(BigDecimal min, BigDecimal max) {
		return new FieldDefinition(key, labelEn, labelAr, type, required, options, min, max);
	}

	FieldDefinition withOptions(List<String> options) {
		return new FieldDefinition(key, labelEn, labelAr, type, required, List.copyOf(options), min, max);
	}

	boolean isNumeric() {
		return type == FieldType.NUMBER || type == FieldType.AMOUNT;
	}

}
