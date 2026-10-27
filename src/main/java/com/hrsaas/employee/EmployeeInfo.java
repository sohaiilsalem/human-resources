package com.hrsaas.employee;

import java.time.LocalDate;

/** Read model other modules use; leave entitlement depends on hire date, birth date, gender and disability. */
public record EmployeeInfo(Long id, String code, String fullNameEn, String fullNameAr, Gender gender,
		LocalDate birthDate, LocalDate hireDate, EmployeeStatus status, LocalDate terminationDate, Long managerId,
		boolean disability, JobTitleInfo jobTitle) {

	public EmployeeInfo(Long id, String code, String fullNameEn, String fullNameAr, Gender gender,
			LocalDate birthDate, LocalDate hireDate, EmployeeStatus status, LocalDate terminationDate, Long managerId,
			boolean disability) {
		this(id, code, fullNameEn, fullNameAr, gender, birthDate, hireDate, status, terminationDate, managerId,
				disability, null);
	}

	public boolean isActive() {
		return status == EmployeeStatus.ACTIVE;
	}

}
