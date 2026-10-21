package com.hrsaas.employee;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeHired(UUID tenantId, UUID employeeId, LocalDate hireDate) {

}
