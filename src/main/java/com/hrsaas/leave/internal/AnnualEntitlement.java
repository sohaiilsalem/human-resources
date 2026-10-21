package com.hrsaas.leave.internal;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.hrsaas.employee.EmployeeInfo;

/**
 * Annual-leave days granted for a calendar leave year.
 *
 * <p>
 * Rules, evaluated as of 31 December of the leave year (in the employee's favour when a threshold is crossed during
 * the year):
 * <ul>
 * <li>nothing until {@code eligibilityMonths} of service have been completed; the grant is usable from that date;</li>
 * <li>less than one year of service: {@code firstYearDays};</li>
 * <li>{@code seniorServiceYears} of service or age {@code seniorAge}: {@code seniorDays};</li>
 * <li>otherwise {@code standardDays};</li>
 * <li>employees with a disability get at least {@code disabilityDays}.</li>
 * </ul>
 * Nothing is granted for a year that starts after the termination date. Pro-rating on exit is a payroll
 * (end-of-service settlement) concern.
 */
final class AnnualEntitlement {

	private AnnualEntitlement() {
	}

	static Optional<Grant> forYear(int year, EmployeeInfo employee, EntitlementRules rules) {
		LocalDate yearStart = LocalDate.of(year, 1, 1);
		LocalDate yearEnd = LocalDate.of(year, 12, 31);
		LocalDate eligibleFrom = eligibleFrom(employee, rules);
		if (eligibleFrom.isAfter(yearEnd)) {
			return Optional.empty();
		}
		LocalDate terminated = employee.terminationDate();
		if (terminated != null && (terminated.isBefore(yearStart) || terminated.isBefore(eligibleFrom))) {
			return Optional.empty();
		}
		long serviceMonths = ChronoUnit.MONTHS.between(employee.hireDate(), yearEnd.plusDays(1));
		int age = Period.between(employee.birthDate(), yearEnd).getYears();

		int days;
		if (serviceMonths < 12) {
			days = rules.firstYearDays();
		}
		else if (serviceMonths >= rules.seniorServiceYears() * 12L || age >= rules.seniorAge()) {
			days = rules.seniorDays();
		}
		else {
			days = rules.standardDays();
		}
		if (employee.disability()) {
			days = Math.max(days, rules.disabilityDays());
		}
		return Optional.of(new Grant(days, eligibleFrom.isAfter(yearStart) ? eligibleFrom : yearStart));
	}

	static LocalDate eligibleFrom(EmployeeInfo employee, EntitlementRules rules) {
		return employee.hireDate().plusMonths(rules.eligibilityMonths());
	}

	record Grant(int days, LocalDate usableFrom) {
	}

}
