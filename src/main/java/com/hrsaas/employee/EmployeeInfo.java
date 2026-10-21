package com.hrsaas.employee;

import java.time.LocalDate;
import java.util.UUID;

/** Read model other modules use; leave entitlement depends on hire date, birth date, gender and disability. */
public record EmployeeInfo(UUID id, String code, String fullNameEn, String fullNameAr, Gender gender,
		LocalDate birthDate, LocalDate hireDate, EmployeeStatus status, LocalDate terminationDate, UUID managerId,
		boolean disability) {

	public boolean isActive() {
		return status == EmployeeStatus.ACTIVE;
	}

}
