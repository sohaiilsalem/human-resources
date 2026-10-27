package com.hrsaas.employee.internal;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.hrsaas.employee.Gender;

record EmployeeRequest(@NotBlank @Size(max = 30) String code, @NotBlank @Size(max = 200) String fullNameEn,
		@NotBlank @Size(max = 200) String fullNameAr,
		@Pattern(regexp = "\\d{14}", message = "{validation.national-id}") String nationalId, @NotNull Gender gender,
		@NotNull @Past LocalDate birthDate, @NotNull LocalDate hireDate, Long managerId, Long jobTitleId,
		boolean disability) {

}
