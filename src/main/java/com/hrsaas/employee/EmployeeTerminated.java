package com.hrsaas.employee;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeTerminated(UUID tenantId, UUID employeeId, LocalDate terminationDate) {

}
