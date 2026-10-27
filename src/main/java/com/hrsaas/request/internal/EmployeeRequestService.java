package com.hrsaas.request.internal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.platform.TenantDirectory;
import com.hrsaas.platform.TenantInfo;
import com.hrsaas.request.RequestApproved;
import com.hrsaas.request.RequestCancelled;
import com.hrsaas.shared.tenant.TenantContext;
import com.hrsaas.shared.web.ApiException;
import com.hrsaas.workflow.ApprovalCompleted;
import com.hrsaas.workflow.ApprovalStatus;
import com.hrsaas.workflow.Approvals;
import com.hrsaas.workflow.StartApproval;

/**
 * Filing, viewing and withdrawing employee requests. Approval is delegated to the workflow engine; its outcome
 * arrives as {@link ApprovalCompleted}.
 */
@Service
@Transactional
class EmployeeRequestService {

	static final String SUBJECT = "REQUEST";

	private final EmployeeRequestRepository requests;

	private final RequestTypeService types;

	private final EmployeeDirectory employees;

	private final TenantDirectory tenants;

	private final Approvals approvals;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	EmployeeRequestService(EmployeeRequestRepository requests, RequestTypeService types, EmployeeDirectory employees,
			TenantDirectory tenants, Approvals approvals, ApplicationEventPublisher events, Clock clock) {
		this.requests = requests;
		this.types = types;
		this.employees = employees;
		this.tenants = tenants;
		this.approvals = approvals;
		this.events = events;
		this.clock = clock;
	}

	/**
	 * For oneself: the type must allow self-service. For someone else: HR ({@code request:manage}) always; a manager
	 * anywhere above the employee in the reporting line if the type allows filing on behalf.
	 */
	EmployeeRequest submit(Long employeeId, RequestInput input, RequestActor actor) {
		RequestType type = types.get(input.type());
		if (!type.isActive()) {
			throw ApiException.unprocessable("request.type-inactive", type.getCode());
		}
		EmployeeInfo employee = employees.findById(employeeId)
			.orElseThrow(() -> ApiException.notFound("employee.not-found"));
		if (!employee.isActive()) {
			throw ApiException.unprocessable("request.employee-inactive");
		}
		if (actor.isEmployee(employeeId)) {
			if (!type.isSelfService() && !actor.canManage()) {
				throw ApiException.forbidden("request.not-self-service", type.getNameEn());
			}
		}
		else if (!actor.canManage() && !(type.isAllowOnBehalf() && isAboveInReportingLine(actor, employeeId))) {
			throw ApiException.forbidden("request.cannot-file-for-employee");
		}
		Map<String, Object> values = RequestForm.validate(type.getFields(), input.data());
		checkMonthlyLimit(type, employeeId);

		EmployeeRequest request = requests.save(new EmployeeRequest(type, employeeId, values,
				blankToNull(input.reason()), actor.userId()));
		approvals.start(new StartApproval(SUBJECT, request.getId(), employeeId, type.getWorkflowCode(), actor.userId(),
				summary(type, values, true), summary(type, values, false),
				RequestForm.attributes(type.getFields(), values)));
		return request;
	}

	/** The employee may withdraw a pending request; HR may cancel a pending or approved one. */
	EmployeeRequest cancel(Long requestId, RequestActor actor, String note) {
		EmployeeRequest request = get(requestId);
		if (!request.isPending() && !request.isApproved()) {
			throw ApiException.conflict("request.not-cancellable");
		}
		boolean ownPending = actor.isEmployee(request.getEmployeeId()) && request.isPending();
		if (!ownPending && !actor.canManage()) {
			throw ApiException.forbidden("request.cannot-cancel");
		}
		boolean wasApproved = request.isApproved();
		request.close(RequestStatus.CANCELLED, clock.instant(), blankToNull(note));
		approvals.cancel(SUBJECT, requestId, actor.userId(), blankToNull(note));
		if (wasApproved) {
			events.publishEvent(new RequestCancelled(TenantContext.require(), requestId, request.getEmployeeId(),
					request.getType().getCode()));
		}
		return request;
	}

	/** The employee, whoever filed it, HR, and everyone taking part in its approval. Others get a 404. */
	@Transactional(readOnly = true)
	EmployeeRequest getVisible(Long requestId, RequestActor actor) {
		EmployeeRequest request = get(requestId);
		if (actor.isEmployee(request.getEmployeeId()) || actor.userId().equals(request.getRequestedBy())
				|| actor.canManage() || approvals.isParticipant(SUBJECT, requestId, actor.userId())) {
			return request;
		}
		throw ApiException.notFound("request.not-found");
	}

	@EventListener
	void onApprovalCompleted(ApprovalCompleted event) {
		if (!SUBJECT.equals(event.subjectType())) {
			return;
		}
		EmployeeRequest request = get(event.subjectId());
		if (event.outcome() == ApprovalStatus.APPROVED) {
			request.close(RequestStatus.APPROVED, clock.instant(), event.note());
			events.publishEvent(new RequestApproved(event.tenantId(), request.getId(), request.getEmployeeId(),
					request.getType().getCode(), request.getType().getCategory(), request.getData()));
		}
		else if (event.outcome() == ApprovalStatus.REJECTED) {
			request.close(RequestStatus.REJECTED, clock.instant(), event.note());
		}
	}

	private EmployeeRequest get(Long requestId) {
		return requests.findById(requestId).orElseThrow(() -> ApiException.notFound("request.not-found"));
	}

	private boolean isAboveInReportingLine(RequestActor actor, Long employeeId) {
		if (actor.employeeId() == null) {
			return false;
		}
		Set<Long> seen = new HashSet<>();
		Long current = employees.findById(employeeId).map(EmployeeInfo::managerId).orElse(null);
		while (current != null && seen.add(current)) {
			if (current.equals(actor.employeeId())) {
				return true;
			}
			current = employees.findById(current).map(EmployeeInfo::managerId).orElse(null);
		}
		return false;
	}

	/** Pending and approved requests of the type filed in the current calendar month (company time zone). */
	private void checkMonthlyLimit(RequestType type, Long employeeId) {
		if (type.getMaxPerMonth() == null) {
			return;
		}
		ZoneId zone = tenants.findById(TenantContext.require())
			.map(TenantInfo::timeZone)
			.map(ZoneId::of)
			.orElse(ZoneId.of("Africa/Cairo"));
		LocalDate firstOfMonth = LocalDate.now(clock.withZone(zone)).withDayOfMonth(1);
		long filed = requests.countInPeriod(employeeId, type.getId(), RequestStatus.ACTIVE,
				firstOfMonth.atStartOfDay(zone).toInstant(), firstOfMonth.plusMonths(1).atStartOfDay(zone).toInstant());
		if (filed >= type.getMaxPerMonth()) {
			throw ApiException.unprocessable("request.monthly-limit", type.getMaxPerMonth());
		}
	}

	/** "Loan · 50000": the type name and the first amount, number or date filled in. */
	private static String summary(RequestType type, Map<String, Object> values, boolean english) {
		String name = english ? type.getNameEn() : type.getNameAr();
		return type.getFields()
			.stream()
			.filter(field -> field.isNumeric() || field.type() == FieldType.DATE)
			.map(field -> values.get(field.key()))
			.filter(value -> value != null)
			.findFirst()
			.map(value -> name + " · "
					+ (value instanceof BigDecimal number ? number.stripTrailingZeros().toPlainString() : value))
			.orElse(name);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
