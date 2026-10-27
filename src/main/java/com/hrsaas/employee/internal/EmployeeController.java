package com.hrsaas.employee.internal;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.billing.Feature;
import com.hrsaas.billing.RequiresFeature;
import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.employee.EmployeeStatus;

@RestController
@RequestMapping("/api/v1/employees")
@RequiresFeature(Feature.CORE_HR)
class EmployeeController {

	private final EmployeeService employees;

	EmployeeController(EmployeeService employees) {
		this.employees = employees;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('employee:read')")
	PagedModel<EmployeeInfo> list(@RequestParam(required = false) EmployeeStatus status, Pageable pageable) {
		return new PagedModel<>(employees.list(status, pageable));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('employee:read')")
	EmployeeInfo get(@PathVariable Long id) {
		return employees.getInfo(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('employee:write')")
	EmployeeInfo hire(@Valid @RequestBody EmployeeRequest request) {
		return employees.hire(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('employee:write')")
	EmployeeInfo update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
		return employees.update(id, request);
	}

	@PostMapping("/{id}/terminate")
	@PreAuthorize("hasAuthority('employee:write')")
	EmployeeInfo terminate(@PathVariable Long id, @Valid @RequestBody Termination request) {
		return employees.terminate(id, request.terminationDate());
	}

	record Termination(@NotNull LocalDate terminationDate) {
	}

}
