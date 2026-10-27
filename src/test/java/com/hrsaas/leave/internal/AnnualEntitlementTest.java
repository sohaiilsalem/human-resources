package com.hrsaas.leave.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.employee.EmployeeStatus;
import com.hrsaas.employee.Gender;

class AnnualEntitlementTest {

	private static final EntitlementRules LAW = EntitlementRules.STATUTORY;

	@Test
	void nothingBeforeSixMonthsOfService() {
		assertThat(AnnualEntitlement.forYear(2026, employee("2026-08-01", "1995-01-01"), LAW)).isEmpty();
	}

	@Test
	void firstYearGetsFifteenDaysUsableAfterSixMonths() {
		var grant = AnnualEntitlement.forYear(2026, employee("2026-03-10", "1995-01-01"), LAW).orElseThrow();
		assertThat(grant.days()).isEqualTo(15);
		assertThat(grant.usableFrom()).isEqualTo(LocalDate.parse("2026-09-10"));
	}

	@Test
	void hiredLateLastYearCompletesAYearThisYear() {
		var grant = AnnualEntitlement.forYear(2026, employee("2025-11-01", "1995-01-01"), LAW).orElseThrow();
		assertThat(grant.days()).isEqualTo(21);
		assertThat(grant.usableFrom()).isEqualTo(LocalDate.parse("2026-05-01"));
	}

	@Test
	void standardTwentyOneDaysUsableFromJanuary() {
		var grant = AnnualEntitlement.forYear(2026, employee("2020-04-01", "1990-06-15"), LAW).orElseThrow();
		assertThat(grant.days()).isEqualTo(21);
		assertThat(grant.usableFrom()).isEqualTo(LocalDate.parse("2026-01-01"));
	}

	@Test
	void thirtyDaysAfterTenYearsOfService() {
		assertThat(AnnualEntitlement.forYear(2026, employee("2016-12-31", "1990-01-01"), LAW).orElseThrow().days())
			.isEqualTo(30);
		assertThat(AnnualEntitlement.forYear(2026, employee("2017-01-02", "1990-01-01"), LAW).orElseThrow().days())
			.isEqualTo(21);
	}

	@Test
	void thirtyDaysFromTheYearTheEmployeeTurnsFifty() {
		assertThat(AnnualEntitlement.forYear(2026, employee("2024-01-01", "1976-12-31"), LAW).orElseThrow().days())
			.isEqualTo(30);
		assertThat(AnnualEntitlement.forYear(2026, employee("2024-01-01", "1977-01-01"), LAW).orElseThrow().days())
			.isEqualTo(21);
	}

	@Test
	void disabilityRaisesTheMinimum() {
		EmployeeInfo employee = new EmployeeInfo(1L, "E1", "A", "أ", Gender.MALE,
				LocalDate.parse("1990-01-01"), LocalDate.parse("2026-01-01"), EmployeeStatus.ACTIVE, null, null, true);
		assertThat(AnnualEntitlement.forYear(2026, employee, LAW).orElseThrow().days()).isEqualTo(45);
	}

	@Test
	void noGrantForYearsAfterTermination() {
		EmployeeInfo employee = new EmployeeInfo(1L, "E1", "A", "أ", Gender.MALE,
				LocalDate.parse("1990-01-01"), LocalDate.parse("2020-01-01"), EmployeeStatus.TERMINATED,
				LocalDate.parse("2025-06-30"), null, false);
		assertThat(AnnualEntitlement.forYear(2025, employee, LAW)).isPresent();
		assertThat(AnnualEntitlement.forYear(2026, employee, LAW)).isEmpty();
	}

	@Test
	void settingsLessFavourableThanTheLawAreDetected() {
		assertThat(LAW.belowStatutory()).isEmpty();
		assertThat(new EntitlementRules(15, 6, 20, 30, 10, 50, 45).belowStatutory()).contains("standardDays");
		assertThat(new EntitlementRules(15, 6, 21, 30, 10, 55, 45).belowStatutory()).contains("seniorAge");
		assertThat(new EntitlementRules(21, 3, 25, 35, 8, 45, 45).belowStatutory()).isEmpty();
	}

	private static EmployeeInfo employee(String hireDate, String birthDate) {
		return new EmployeeInfo(1L, "E1", "A", "أ", Gender.MALE, LocalDate.parse(birthDate),
				LocalDate.parse(hireDate), EmployeeStatus.ACTIVE, null, null, false);
	}

}
