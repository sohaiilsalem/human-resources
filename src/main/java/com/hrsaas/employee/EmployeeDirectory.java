package com.hrsaas.employee;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Employees of the current tenant. */
public interface EmployeeDirectory {

	Optional<EmployeeInfo> findById(UUID id);

	Map<UUID, EmployeeInfo> findAllById(Collection<UUID> ids);

	List<UUID> findActiveIds();

	List<UUID> findDirectReportIds(UUID managerId);

}
