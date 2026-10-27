package com.hrsaas.request.internal;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.hrsaas.request.RequestCategory;

/** Definition of a request type, as created and edited by HR ({@code request:manage}). */
record RequestTypeRequest(@NotBlank @Pattern(regexp = "[A-Z0-9_]{2,30}", message = "{validation.code}") String code,
		@NotBlank @Size(max = 100) String nameEn, @NotBlank @Size(max = 100) String nameAr,
		@Size(max = 500) String descriptionEn, @Size(max = 500) String descriptionAr,
		@NotNull RequestCategory category, @NotNull @Size(max = 30) List<@Valid @NotNull FieldDefinition> fields,
		String workflowCode, boolean selfService, boolean allowOnBehalf, @Min(1) Integer maxPerMonth, boolean active,
		int sortOrder) {

}
