package com.hrsaas.employee;

import java.time.LocalDate;

public record EmployeeHired(Long tenantId, Long employeeId, LocalDate hireDate) {

}
