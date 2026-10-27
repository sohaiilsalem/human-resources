package com.hrsaas.employee;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Employees of the current tenant. */
public interface EmployeeDirectory {

	Optional<EmployeeInfo> findById(Long id);

	Map<Long, EmployeeInfo> findAllById(Collection<Long> ids);

	List<Long> findActiveIds();

	List<Long> findDirectReportIds(Long managerId);

}
