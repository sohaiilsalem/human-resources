package com.hrsaas.employee;

import java.time.LocalDate;

public record EmployeeTerminated(Long tenantId, Long employeeId, LocalDate terminationDate) {

}
