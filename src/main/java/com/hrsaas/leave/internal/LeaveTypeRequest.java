package com.hrsaas.leave.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.hrsaas.employee.Gender;
import com.hrsaas.leave.LeaveCategory;

/**
 * Definition of a leave type. {@code deductsFrom} names another type whose balance is consumed (casual leave counts
 * against annual leave). {@code workflowCode} names the approval workflow: omitted (null) keeps the current one
 * (none on create, i.e. the company default), blank resets to the company default.
 */
record LeaveTypeRequest(@NotBlank @Pattern(regexp = "[A-Z0-9_]{2,30}", message = "{validation.code}") String code,
		@NotBlank @Size(max = 100) String nameEn, @NotBlank @Size(max = 100) String nameAr,
		@NotNull LeaveCategory category, boolean paid, boolean tracksBalance, String deductsFrom,
		@NotNull DayCountMode dayCountMode, boolean allowHalfDay, @Min(1) Integer maxDaysPerRequest,
		@Min(1) Integer maxDaysPerYear, @Min(1) Integer maxTimesPerService, @Min(0) Integer minServiceMonths,
		Gender gender, boolean requiresAttachment, boolean active, String workflowCode) {

}
