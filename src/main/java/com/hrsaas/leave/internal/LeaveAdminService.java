package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.leave.internal.LeaveViews.SettingsView;
import com.hrsaas.shared.web.ApiException;

/** Leave types, entitlement settings and manual balance adjustments ({@code leave:manage}). */
@Service
@Transactional
class LeaveAdminService {

	private final LeaveTypeRepository types;

	private final LeaveSettingsRepository settings;

	private final LeaveTransactionRepository ledger;

	private final EmployeeDirectory employees;

	private final TenantToday today;

	LeaveAdminService(LeaveTypeRepository types, LeaveSettingsRepository settings, LeaveTransactionRepository ledger,
			EmployeeDirectory employees, TenantToday today) {
		this.types = types;
		this.settings = settings;
		this.ledger = ledger;
		this.employees = employees;
		this.today = today;
	}

	@Transactional(readOnly = true)
	List<LeaveType> types() {
		return types.findAllOrdered();
	}

	LeaveType createType(LeaveTypeRequest request) {
		if (types.existsByCode(request.code())) {
			throw ApiException.conflict("leave.type-code-taken", request.code());
		}
		return types.save(new LeaveType(request, deductsFrom(request, null)));
	}

	/** Code and category are fixed once created; everything else can change. */
	LeaveType updateType(String code, LeaveTypeRequest request) {
		LeaveType type = types.findByCode(code).orElseThrow(() -> ApiException.notFound("leave.type-not-found", code));
		if (!type.getCode().equals(request.code()) || type.getCategory() != request.category()) {
			throw ApiException.unprocessable("leave.type-immutable-fields");
		}
		type.apply(request, deductsFrom(request, type));
		return type;
	}

	@Transactional(readOnly = true)
	SettingsView settings() {
		return SettingsView.of(settings.findCurrent().orElseGet(LeaveSettings::new));
	}

	SettingsView updateSettings(SettingsView request) {
		EntitlementRules rules = request.rules();
		rules.belowStatutory().ifPresent(field -> {
			throw ApiException.unprocessable("leave.settings-below-statutory", field);
		});
		LeaveSettings current = settings.findCurrent().orElseGet(() -> settings.save(new LeaveSettings()));
		current.apply(rules, request.allowNegativeBalance());
		return SettingsView.of(current);
	}

	/** Opening balances, carried-over days or corrections. Positive adds days, negative removes them. */
	LeaveTransaction adjust(Long employeeId, String leaveTypeCode, BigDecimal days, String note) {
		employees.findById(employeeId).orElseThrow(() -> ApiException.notFound("employee.not-found"));
		LeaveType type = types.findByCode(leaveTypeCode)
			.orElseThrow(() -> ApiException.notFound("leave.type-not-found", leaveTypeCode));
		if (!type.hasOwnLedger()) {
			throw ApiException.unprocessable("leave.type-has-no-balance", leaveTypeCode);
		}
		ledger.lockEmployee(employeeId);
		return ledger.save(new LeaveTransaction(employeeId, type.getId(), today.get().getYear(),
				TransactionKind.ADJUSTMENT, days, today.get(), null, note.strip()));
	}

	private LeaveType deductsFrom(LeaveTypeRequest request, LeaveType self) {
		if (request.deductsFrom() == null || request.deductsFrom().isBlank()) {
			return null;
		}
		LeaveType target = types.findByCode(request.deductsFrom())
			.orElseThrow(() -> ApiException.unprocessable("leave.type-not-found", request.deductsFrom()));
		if (target.equals(self) || !target.hasOwnLedger()) {
			throw ApiException.unprocessable("leave.invalid-deducts-from", request.deductsFrom());
		}
		return target;
	}

}
