package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.leave.LeaveCategory;
import com.hrsaas.leave.internal.LeaveViews.BalanceView;
import com.hrsaas.leave.internal.LeaveViews.RequestView;
import com.hrsaas.leave.internal.LeaveViews.TransactionView;
import com.hrsaas.shared.web.ApiException;

/** Balances, request lists and ledger history. Balances first bring the year's entitlement up to date. */
@Service
@Transactional
class LeaveQueryService {

	private final LeaveTypeRepository types;

	private final LeaveRequestRepository requests;

	private final LeaveTransactionRepository ledger;

	private final EntitlementService entitlements;

	private final EmployeeDirectory employees;

	private final TenantToday today;

	LeaveQueryService(LeaveTypeRepository types, LeaveRequestRepository requests, LeaveTransactionRepository ledger,
			EntitlementService entitlements, EmployeeDirectory employees, TenantToday today) {
		this.types = types;
		this.requests = requests;
		this.ledger = ledger;
		this.entitlements = entitlements;
		this.employees = employees;
		this.today = today;
	}

	List<BalanceView> balances(UUID employeeId) {
		EmployeeInfo employee = employee(employeeId);
		int year = today.get().getYear();
		entitlements.syncAnnual(employee, year);
		List<BalanceView> views = new ArrayList<>();
		for (LeaveType type : types.findAllOrdered()) {
			if (!type.isActive()) {
				continue;
			}
			BigDecimal used = requests.daysInYear(employeeId, type.getId(), year,
					Set.of(LeaveRequestStatus.APPROVED));
			if (!type.hasOwnLedger()) {
				views.add(new BalanceView(type.getCode(), type.getNameEn(), type.getNameAr(), type.getCategory(),
						type.isPaid(), type.getDeductsFrom() != null ? type.getDeductsFrom().getCode() : null, null,
						null, null, null, used, type.getMaxDaysPerYear()));
				continue;
			}
			BigDecimal entitled = type.getCategory() == LeaveCategory.ANNUAL ? ledger
				.findByEmployeeIdAndLeaveTypeIdAndLeaveYearAndKind(employeeId, type.getId(), year,
						TransactionKind.ENTITLEMENT)
				.map(LeaveTransaction::getDays)
				.orElse(BigDecimal.ZERO) : null;
			BigDecimal balance = ledger.balance(employeeId, type.getId(), year);
			BigDecimal pending = requests.pendingDays(employeeId, type.getId());
			views.add(new BalanceView(type.getCode(), type.getNameEn(), type.getNameAr(), type.getCategory(),
					type.isPaid(), null, entitled, balance, pending, balance.subtract(pending), used,
					type.getMaxDaysPerYear()));
		}
		return views;
	}

	@Transactional(readOnly = true)
	Page<RequestView> requestsOf(UUID employeeId, Pageable pageable) {
		return withEmployees(requests.findByEmployeeId(employeeId, pageable));
	}

	/** HR sees every request; a manager sees their direct reports'. */
	@Transactional(readOnly = true)
	Page<RequestView> search(Actor actor, LeaveRequestStatus status, UUID employeeId, Pageable pageable) {
		if (actor.canManage()) {
			return withEmployees(requests.search(status, employeeId, pageable));
		}
		if (actor.canApprove() && actor.employeeId() != null) {
			List<UUID> team = employees.findDirectReportIds(actor.employeeId());
			if (employeeId != null) {
				team = team.contains(employeeId) ? List.of(employeeId) : List.of();
			}
			if (team.isEmpty()) {
				return Page.empty(pageable);
			}
			return withEmployees(requests.searchAmong(team, status, pageable));
		}
		throw ApiException.forbidden("leave.not-your-team");
	}

	@Transactional(readOnly = true)
	RequestView view(LeaveRequest request) {
		return RequestView.of(request, employees.findById(request.getEmployeeId()).orElse(null));
	}

	@Transactional(readOnly = true)
	List<TransactionView> history(UUID employeeId) {
		employee(employeeId);
		Map<UUID, String> codes = types.findAll()
			.stream()
			.collect(Collectors.toMap(LeaveType::getId, LeaveType::getCode));
		return ledger.history(employeeId)
			.stream()
			.map(t -> new TransactionView(t.getId(), codes.get(t.getLeaveTypeId()), t.getLeaveYear(), t.getKind(),
					t.getDays(), t.getEffectiveDate(), t.getRequestId(), t.getNote(), t.getCreatedAt()))
			.toList();
	}

	/** Whether the actor may see this employee's balances: themselves, HR, or their manager. */
	boolean canSeeEmployee(Actor actor, UUID employeeId) {
		return actor.isEmployee(employeeId) || actor.canManage() || (actor.canApprove() && actor.employeeId() != null
				&& employee(employeeId).managerId() != null
				&& employee(employeeId).managerId().equals(actor.employeeId()));
	}

	private EmployeeInfo employee(UUID employeeId) {
		return employees.findById(employeeId).orElseThrow(() -> ApiException.notFound("employee.not-found"));
	}

	private Page<RequestView> withEmployees(Page<LeaveRequest> page) {
		Map<UUID, EmployeeInfo> byId = employees
			.findAllById(page.getContent().stream().map(LeaveRequest::getEmployeeId).collect(Collectors.toSet()));
		return page.map(request -> RequestView.of(request, byId.get(request.getEmployeeId())));
	}

}
