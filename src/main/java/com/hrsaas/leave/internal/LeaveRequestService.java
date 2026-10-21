package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.leave.LeaveApproved;
import com.hrsaas.leave.LeaveCancelled;
import com.hrsaas.leave.LeaveCategory;
import com.hrsaas.organization.WorkCalendar;
import com.hrsaas.shared.tenant.TenantContext;
import com.hrsaas.shared.web.ApiException;

/**
 * Leave request lifecycle: {@code PENDING → APPROVED | REJECTED}, and {@code PENDING | APPROVED → CANCELLED}.
 * Balance-affecting steps lock the employee so concurrent requests cannot overspend.
 */
@Service
@Transactional
class LeaveRequestService {

	private final LeaveRequestRepository requests;

	private final LeaveTypeRepository types;

	private final LeaveTransactionRepository ledger;

	private final EntitlementService entitlements;

	private final EmployeeDirectory employees;

	private final WorkCalendar calendar;

	private final TenantToday today;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	LeaveRequestService(LeaveRequestRepository requests, LeaveTypeRepository types, LeaveTransactionRepository ledger,
			EntitlementService entitlements, EmployeeDirectory employees, WorkCalendar calendar, TenantToday today,
			ApplicationEventPublisher events, Clock clock) {
		this.requests = requests;
		this.types = types;
		this.ledger = ledger;
		this.entitlements = entitlements;
		this.employees = employees;
		this.calendar = calendar;
		this.today = today;
		this.events = events;
		this.clock = clock;
	}

	LeaveRequest submit(UUID employeeId, LeaveRequestInput input, Actor actor) {
		ledger.lockEmployee(employeeId);
		EmployeeInfo employee = employees.findById(employeeId)
			.orElseThrow(() -> ApiException.notFound("employee.not-found"));
		if (!employee.isActive()) {
			throw ApiException.unprocessable("leave.employee-inactive");
		}
		LeaveType type = types.findByCode(input.leaveType())
			.orElseThrow(() -> ApiException.notFound("leave.type-not-found", input.leaveType()));
		if (!type.isActive()) {
			throw ApiException.unprocessable("leave.type-inactive", type.getCode());
		}
		LocalDate start = input.startDate();
		LocalDate end = input.endDate();
		if (end.isBefore(start)) {
			throw ApiException.unprocessable("leave.invalid-dates");
		}
		if (input.halfDay() && (!type.isAllowHalfDay() || !start.equals(end))) {
			throw ApiException.unprocessable("leave.half-day-not-allowed");
		}
		BigDecimal days = LeaveDays.count(type.getDayCountMode(), calendar.between(start, end), start, end,
				input.halfDay());
		if (days.signum() == 0) {
			throw ApiException.unprocessable("leave.no-working-days");
		}
		checkEligibility(employee, type, start, days);
		if (requests.overlaps(employeeId, start, end, LeaveRequestStatus.ACTIVE)) {
			throw ApiException.conflict("leave.overlap");
		}
		LeaveType ledgerType = type.ledgerType();
		if (ledgerType != null) {
			checkBalance(employee, ledgerType, start.getYear(), days, BigDecimal.ZERO);
		}
		return requests.save(new LeaveRequest(employeeId, type, start, end, input.halfDay(), days,
				blankToNull(input.reason()), actor.userId()));
	}

	LeaveRequest approve(UUID requestId, Actor actor, String note) {
		LeaveRequest request = get(requestId);
		requirePending(request);
		authorizeDecision(request, actor);
		ledger.lockEmployee(request.getEmployeeId());
		LeaveType ledgerType = request.getLeaveType().ledgerType();
		if (ledgerType != null) {
			EmployeeInfo employee = employees.findById(request.getEmployeeId()).orElseThrow();
			// this request is still pending, so it is already counted in the pending days
			checkBalance(employee, ledgerType, request.getLeaveYear(), request.getDays(), request.getDays());
			ledger.save(new LeaveTransaction(request.getEmployeeId(), ledgerType.getId(), request.getLeaveYear(),
					TransactionKind.USAGE, request.getDays().negate(), request.getStartDate(), request.getId(), null));
		}
		request.decide(LeaveRequestStatus.APPROVED, actor.userId(), clock.instant(), blankToNull(note));
		LeaveType type = request.getLeaveType();
		events.publishEvent(new LeaveApproved(TenantContext.require(), request.getId(), request.getEmployeeId(),
				type.getCode(), type.getCategory(), type.isPaid(), request.getStartDate(), request.getEndDate(),
				request.isHalfDay(), request.getDays()));
		return request;
	}

	LeaveRequest reject(UUID requestId, Actor actor, String note) {
		LeaveRequest request = get(requestId);
		requirePending(request);
		authorizeDecision(request, actor);
		request.decide(LeaveRequestStatus.REJECTED, actor.userId(), clock.instant(), blankToNull(note));
		return request;
	}

	/**
	 * The employee may withdraw a pending request, or an approved one that has not started yet. HR ({@code leave:manage})
	 * may cancel any pending or approved request; approved days go back to the balance.
	 */
	LeaveRequest cancel(UUID requestId, Actor actor, String note) {
		LeaveRequest request = get(requestId);
		if (!request.isPending() && !request.isApproved()) {
			throw ApiException.conflict("leave.not-cancellable");
		}
		boolean ownNotStarted = actor.isEmployee(request.getEmployeeId())
				&& (request.isPending() || request.getStartDate().isAfter(today.get()));
		if (!ownNotStarted && !actor.canManage()) {
			throw ApiException.forbidden("leave.cannot-cancel");
		}
		boolean wasApproved = request.isApproved();
		ledger.lockEmployee(request.getEmployeeId());
		request.decide(LeaveRequestStatus.CANCELLED, actor.userId(), clock.instant(), blankToNull(note));
		LeaveType ledgerType = request.getLeaveType().ledgerType();
		if (wasApproved && ledgerType != null) {
			ledger.save(new LeaveTransaction(request.getEmployeeId(), ledgerType.getId(), request.getLeaveYear(),
					TransactionKind.REVERSAL, request.getDays(), today.get(), request.getId(), null));
		}
		if (wasApproved) {
			events.publishEvent(new LeaveCancelled(TenantContext.require(), request.getId(), request.getEmployeeId(),
					request.getStartDate(), request.getEndDate()));
		}
		return request;
	}

	@Transactional(readOnly = true)
	LeaveRequest getVisible(UUID requestId, Actor actor) {
		LeaveRequest request = get(requestId);
		if (actor.isEmployee(request.getEmployeeId()) || actor.canManage() || isManagerOf(actor, request)) {
			return request;
		}
		throw ApiException.notFound("leave.request-not-found");
	}

	private LeaveRequest get(UUID requestId) {
		return requests.findById(requestId).orElseThrow(() -> ApiException.notFound("leave.request-not-found"));
	}

	private static void requirePending(LeaveRequest request) {
		if (!request.isPending()) {
			throw ApiException.conflict("leave.not-pending");
		}
	}

	/** Never one's own request; HR may decide any, a manager only their direct reports'. */
	private void authorizeDecision(LeaveRequest request, Actor actor) {
		if (actor.isEmployee(request.getEmployeeId())) {
			throw ApiException.forbidden("leave.cannot-decide-own");
		}
		if (actor.canManage()) {
			return;
		}
		if (!actor.canApprove() || !isManagerOf(actor, request)) {
			throw ApiException.forbidden("leave.not-your-team");
		}
	}

	private boolean isManagerOf(Actor actor, LeaveRequest request) {
		return actor.employeeId() != null && employees.findById(request.getEmployeeId())
			.map(EmployeeInfo::managerId)
			.filter(actor.employeeId()::equals)
			.isPresent();
	}

	private void checkEligibility(EmployeeInfo employee, LeaveType type, LocalDate start, BigDecimal days) {
		if (start.isBefore(employee.hireDate())) {
			throw ApiException.unprocessable("leave.before-hire-date");
		}
		if (type.getGender() != null && type.getGender() != employee.gender()) {
			throw ApiException.unprocessable("leave.gender-restricted");
		}
		if (type.getMinServiceMonths() != null
				&& ChronoUnit.MONTHS.between(employee.hireDate(), start) < type.getMinServiceMonths()) {
			throw ApiException.unprocessable("leave.min-service", type.getMinServiceMonths());
		}
		LeaveType ledgerType = type.ledgerType();
		if (ledgerType != null && ledgerType.getCategory() == LeaveCategory.ANNUAL) {
			LocalDate eligibleFrom = AnnualEntitlement.eligibleFrom(employee, entitlements.settings().rules());
			if (start.isBefore(eligibleFrom)) {
				throw ApiException.unprocessable("leave.not-eligible-yet", eligibleFrom);
			}
		}
		if (type.getMaxDaysPerRequest() != null && days.compareTo(BigDecimal.valueOf(type.getMaxDaysPerRequest())) > 0) {
			throw ApiException.unprocessable("leave.max-days-per-request", type.getMaxDaysPerRequest());
		}
		if (type.getMaxDaysPerYear() != null) {
			BigDecimal taken = requests.daysInYear(employee.id(), type.getId(), start.getYear(),
					LeaveRequestStatus.ACTIVE);
			if (taken.add(days).compareTo(BigDecimal.valueOf(type.getMaxDaysPerYear())) > 0) {
				throw ApiException.unprocessable("leave.max-days-per-year", type.getMaxDaysPerYear(), taken);
			}
		}
		if (type.getMaxTimesPerService() != null && requests.countByEmployeeIdAndLeaveTypeIdAndStatusIn(employee.id(),
				type.getId(), LeaveRequestStatus.ACTIVE) >= type.getMaxTimesPerService()) {
			throw ApiException.unprocessable("leave.max-times-per-service", type.getMaxTimesPerService());
		}
	}

	/**
	 * @param alreadyPending days of this same request already included in the pending total (when approving)
	 */
	private void checkBalance(EmployeeInfo employee, LeaveType ledgerType, int year, BigDecimal days,
			BigDecimal alreadyPending) {
		if (ledgerType.getCategory() == LeaveCategory.ANNUAL) {
			entitlements.sync(employee, ledgerType, year);
		}
		if (entitlements.settings().isAllowNegativeBalance()) {
			return;
		}
		BigDecimal available = ledger.balance(employee.id(), ledgerType.getId(), year)
			.subtract(requests.pendingDays(employee.id(), ledgerType.getId()).subtract(alreadyPending));
		if (days.compareTo(available) > 0) {
			throw ApiException.unprocessable("leave.insufficient-balance", available.stripTrailingZeros().toPlainString());
		}
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
