package com.hrsaas.request.internal;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.request.internal.RequestViews.RequestView;
import com.hrsaas.workflow.Approvals;

@Service
@Transactional(readOnly = true)
class RequestQueryService {

	private final EmployeeRequestRepository requests;

	private final EmployeeDirectory employees;

	private final Approvals approvals;

	RequestQueryService(EmployeeRequestRepository requests, EmployeeDirectory employees, Approvals approvals) {
		this.requests = requests;
		this.employees = employees;
		this.approvals = approvals;
	}

	Page<RequestView> requestsOf(Long employeeId, Pageable pageable) {
		return withEmployees(requests.findByEmployeeId(employeeId, pageable));
	}

	Page<RequestView> search(RequestStatus status, String type, Long employeeId, Pageable pageable) {
		return withEmployees(requests.search(status, type, employeeId, pageable));
	}

	RequestView detail(EmployeeRequest request) {
		return RequestView.of(request, employees.findById(request.getEmployeeId()).orElse(null),
				approvals.find(EmployeeRequestService.SUBJECT, request.getId()).orElse(null));
	}

	private Page<RequestView> withEmployees(Page<EmployeeRequest> page) {
		Map<Long, EmployeeInfo> byId = employees
			.findAllById(page.getContent().stream().map(EmployeeRequest::getEmployeeId).collect(Collectors.toSet()));
		return page.map(request -> RequestView.of(request, byId.get(request.getEmployeeId()), null));
	}

}
