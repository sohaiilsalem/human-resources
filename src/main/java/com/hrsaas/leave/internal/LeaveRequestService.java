package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
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
import com.hrsaas.workflow.ApprovalCompleted;
import com.hrsaas.workflow.ApprovalStatus;
import com.hrsaas.workflow.Approvals;
import com.hrsaas.workflow.Decision;
import com.hrsaas.workflow.StartApproval;

/**
 * Leave request lifecycle: {@code PENDING → APPROVED | REJECTED}, and {@code PENDING | APPROVED → CANCELLED}.
 * Who approves, and in which order, is up to the leave type's approval workflow; the final outcome arrives as
 * {@link ApprovalCompleted}. Balance-affecting steps lock the employee so concurrent requests cannot overspend.
 */
@Service
@Transactional
class LeaveRequestService {

	/** Subject type of leave requests in the approval engine. */
	static final String SUBJECT = "LEAVE";

	private final LeaveRequestRepository requests;

	private final LeaveTypeRepository types;

	private final LeaveTransactionRepository ledger;

	private final EntitlementService entitlements;

	private final EmployeeDirectory employees;

	private final WorkCalendar calendar;

	private final TenantToday today;

	private final Approvals approvals;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	LeaveRequestService(LeaveRequestRepository requests, LeaveTypeRepository types, LeaveTransactionRepository ledger,
			EntitlementService entitlements, EmployeeDirectory employees, WorkCalendar calendar, TenantToday today,
			Approvals approvals, ApplicationEventPublisher events, Clock clock) {
		this.requests = requests;
		this.types = types;
		this.ledger = ledger;
		this.entitlements = entitlements;
		this.employees = employees;
		this.calendar = calendar;
		this.today = today;
		this.approvals = approvals;
		this.events = events;
		this.clock = clock;
	}

	LeaveRequest submit(Long employeeId, LeaveRequestInput input, Actor actor) {
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
		LeaveRequest request = requests.save(new LeaveRequest(employeeId, type, start, end, input.halfDay(), days,
				blankToNull(input.reason()), actor.userId()));
		approvals.start(approval(request, actor.userId()));
		return request;
	}

	LeaveRequest approve(Long requestId, Actor actor, String note) {
		return decide(requestId, actor, Decision.APPROVE, note);
	}

	LeaveRequest reject(Long requestId, Actor actor, String note) {
		return decide(requestId, actor, Decision.REJECT, note);
	}

	/**
	 * Records the actor's decision on the current approval step. The request itself changes only when the approval
	 * completes (see {@link #onApprovalCompleted}), which may take several steps.
	 */
	private LeaveRequest decide(Long requestId, Actor actor, Decision decision, String note) {
		LeaveRequest request = get(requestId);
		requirePending(request);
		if (approvals.find(SUBJECT, requestId).isEmpty()) {
			// filed before approval workflows existed: give it one now
			approvals.start(approval(request, request.getRequestedBy()));
		}
		approvals.decide(SUBJECT, requestId, actor.userId(), decision, note);
		return request;
	}

	/** Pending requests filed before approval workflows existed get their approval, so they reach the inbox. */
	int startMissingApprovals() {
		int started = 0;
		for (LeaveRequest request : requests.findByStatus(LeaveRequestStatus.PENDING)) {
			if (approvals.find(SUBJECT, request.getId()).isEmpty()) {
				approvals.start(approval(request, request.getRequestedBy()));
				started++;
			}
		}
		return started;
	}

	@EventListener
	void onApprovalCompleted(ApprovalCompleted event) {
		if (!SUBJECT.equals(event.subjectType())) {
			return;
		}
		LeaveRequest request = get(event.subjectId());
		requirePending(request);
		if (event.outcome() == ApprovalStatus.APPROVED) {
			applyApproval(request, event.decidedBy(), event.note());
		}
		else if (event.outcome() == ApprovalStatus.REJECTED) {
			request.decide(LeaveRequestStatus.REJECTED, event.decidedBy(), clock.instant(), event.note());
		}
	}

	/** Final approval: draws the days from the balance (checked again, since it may have changed meanwhile). */
	private void applyApproval(LeaveRequest request, Long decidedBy, String note) {
		ledger.lockEmployee(request.getEmployeeId());
		LeaveType ledgerType = request.getLeaveType().ledgerType();
		if (ledgerType != null) {
			EmployeeInfo employee = employees.findById(request.getEmployeeId()).orElseThrow();
			// this request is still pending, so it is already counted in the pending days
			checkBalance(employee, ledgerType, request.getLeaveYear(), request.getDays(), request.getDays());
			ledger.save(new LeaveTransaction(request.getEmployeeId(), ledgerType.getId(), request.getLeaveYear(),
					TransactionKind.USAGE, request.getDays().negate(), request.getStartDate(), request.getId(), null));
		}
		request.decide(LeaveRequestStatus.APPROVED, decidedBy, clock.instant(), note);
		LeaveType type = request.getLeaveType();
		events.publishEvent(new LeaveApproved(TenantContext.require(), request.getId(), request.getEmployeeId(),
				type.getCode(), type.getCategory(), type.isPaid(), request.getStartDate(), request.getEndDate(),
				request.isHalfDay(), request.getDays()));
	}

	/** "Annual leave · 3 days · 2026-11-02 → 2026-11-04"; {@code days} feeds step conditions. */
	private static StartApproval approval(LeaveRequest request, Long requestedBy) {
		LeaveType type = request.getLeaveType();
		String days = request.getDays().stripTrailingZeros().toPlainString();
		String period = request.getStartDate().equals(request.getEndDate()) ? request.getStartDate().toString()
				: request.getStartDate() + " → " + request.getEndDate();
		return new StartApproval(SUBJECT, request.getId(), request.getEmployeeId(), type.getWorkflowCode(),
				requestedBy, type.getNameEn() + " · " + days + (days.equals("1") ? " day · " : " days · ") + period,
				type.getNameAr() + " · " + days + " يوم · " + period, Map.of("days", request.getDays()));
	}

	/**
	 * The employee may withdraw a pending request, or an approved one that has not started yet. HR ({@code leave:manage})
	 * may cancel any pending or approved request; approved days go back to the balance.
	 */
	LeaveRequest cancel(Long requestId, Actor actor, String note) {
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
		approvals.cancel(SUBJECT, requestId, actor.userId(), blankToNull(note));
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
	LeaveRequest getVisible(Long requestId, Actor actor) {
		LeaveRequest request = get(requestId);
		if (actor.isEmployee(request.getEmployeeId()) || actor.canManage() || isManagerOf(actor, request)
				|| approvals.isParticipant(SUBJECT, requestId, actor.userId())) {
			return request;
		}
		throw ApiException.notFound("leave.request-not-found");
	}

	private LeaveRequest get(Long requestId) {
		return requests.findById(requestId).orElseThrow(() -> ApiException.notFound("leave.request-not-found"));
	}

	private static void requirePending(LeaveRequest request) {
		if (!request.isPending()) {
			throw ApiException.conflict("leave.not-pending");
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
