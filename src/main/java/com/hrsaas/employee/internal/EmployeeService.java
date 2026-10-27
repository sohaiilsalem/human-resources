package com.hrsaas.employee.internal;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.employee.EmployeeHired;
import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.employee.EmployeeStatus;
import com.hrsaas.employee.EmployeeTerminated;
import com.hrsaas.shared.tenant.TenantContext;
import com.hrsaas.shared.web.ApiException;

@Service
@Transactional(readOnly = true)
class EmployeeService implements EmployeeDirectory {

	private final EmployeeRepository employees;
	private final JobTitleRepository jobTitles;

	private final ApplicationEventPublisher events;

	EmployeeService(EmployeeRepository employees, JobTitleRepository jobTitles, ApplicationEventPublisher events) {
		this.employees = employees;
		this.jobTitles = jobTitles;
		this.events = events;
	}

	@Override
	public Optional<EmployeeInfo> findById(Long id) {
		return employees.findById(id).map(Employee::toInfo);
	}

	@Override
	public Map<Long, EmployeeInfo> findAllById(Collection<Long> ids) {
		return employees.findAllById(ids)
			.stream()
			.map(Employee::toInfo)
			.collect(Collectors.toMap(EmployeeInfo::id, Function.identity()));
	}

	@Override
	public List<Long> findActiveIds() {
		return employees.findActiveIds();
	}

	@Override
	public List<Long> findDirectReportIds(Long managerId) {
		return employees.findDirectReportIds(managerId);
	}

	Page<EmployeeInfo> list(EmployeeStatus status, Pageable pageable) {
		Page<Employee> page = status == null ? employees.findAll(pageable) : employees.findByStatus(status, pageable);
		return page.map(Employee::toInfo);
	}

	Employee get(Long id) {
		return employees.findById(id).orElseThrow(() -> ApiException.notFound("employee.not-found"));
	}

	EmployeeInfo getInfo(Long id) {
		return get(id).toInfo();
	}

	@Transactional
	EmployeeInfo hire(EmployeeRequest request) {
		if (employees.existsByCode(request.code().strip())) {
			throw ApiException.conflict("employee.code-taken", request.code());
		}
		validateManager(request.managerId(), null);
		Employee employee = employees.save(new Employee(request, resolveJobTitle(request.jobTitleId())));
		events.publishEvent(new EmployeeHired(TenantContext.require(), employee.getId(), request.hireDate()));
		return employee.toInfo();
	}

	@Transactional
	EmployeeInfo update(Long id, EmployeeRequest request) {
		Employee employee = get(id);
		if (employees.existsByCodeAndIdNot(request.code().strip(), id)) {
			throw ApiException.conflict("employee.code-taken", request.code());
		}
		validateManager(request.managerId(), id);
		JobTitle jobTitle = request.jobTitleId() == null ? employee.getJobTitle()
				: resolveJobTitle(request.jobTitleId());
		employee.apply(request, jobTitle);
		return employee.toInfo();
	}

	@Transactional
	EmployeeInfo terminate(Long id, LocalDate date) {
		Employee employee = get(id);
		if (!employee.isActive()) {
			throw ApiException.conflict("employee.already-terminated");
		}
		employee.terminate(date);
		events.publishEvent(new EmployeeTerminated(TenantContext.require(), id, date));
		return employee.toInfo();
	}

	private void validateManager(Long managerId, Long employeeId) {
		if (managerId == null) {
			return;
		}
		if (managerId.equals(employeeId) || !employees.existsById(managerId)) {
			throw ApiException.unprocessable("employee.invalid-manager");
		}
	}

	private JobTitle resolveJobTitle(Long jobTitleId) {
		if (jobTitleId == null) {
			return null;
		}
		return jobTitles.findById(jobTitleId)
			.orElseThrow(() -> ApiException.unprocessable("job-title.not-found", jobTitleId));
	}

}
