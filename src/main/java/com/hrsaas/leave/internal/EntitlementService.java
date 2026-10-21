package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.leave.LeaveCategory;

/** Keeps each employee's yearly annual-leave grant in the ledger in line with the rules and their record. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class EntitlementService {

	private final LeaveTransactionRepository ledger;

	private final LeaveTypeRepository types;

	private final LeaveSettingsRepository settings;

	EntitlementService(LeaveTransactionRepository ledger, LeaveTypeRepository types, LeaveSettingsRepository settings) {
		this.ledger = ledger;
		this.types = types;
		this.settings = settings;
	}

	Optional<LeaveType> annualType() {
		return types.findFirstByCategoryAndActiveTrueAndDeductsFromIsNull(LeaveCategory.ANNUAL);
	}

	LeaveSettings settings() {
		return settings.findCurrent().orElseGet(LeaveSettings::new);
	}

	/** Idempotent: creates, corrects or removes the {@code ENTITLEMENT} row for that year. */
	void sync(EmployeeInfo employee, LeaveType annual, int year) {
		Optional<AnnualEntitlement.Grant> grant = AnnualEntitlement.forYear(year, employee, settings().rules());
		Optional<LeaveTransaction> existing = ledger.findByEmployeeIdAndLeaveTypeIdAndLeaveYearAndKind(employee.id(),
				annual.getId(), year, TransactionKind.ENTITLEMENT);
		if (grant.isEmpty()) {
			existing.ifPresent(ledger::delete);
			return;
		}
		BigDecimal days = BigDecimal.valueOf(grant.get().days());
		if (existing.isEmpty()) {
			ledger.save(new LeaveTransaction(employee.id(), annual.getId(), year, TransactionKind.ENTITLEMENT, days,
					grant.get().usableFrom(), null, null));
		}
		else if (existing.get().getDays().compareTo(days) != 0
				|| !existing.get().getEffectiveDate().equals(grant.get().usableFrom())) {
			existing.get().resync(days, grant.get().usableFrom());
		}
	}

	void syncAnnual(EmployeeInfo employee, int year) {
		annualType().ifPresent(annual -> sync(employee, annual, year));
	}

}
