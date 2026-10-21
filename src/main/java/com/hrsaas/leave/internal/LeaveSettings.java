package com.hrsaas.leave.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;

/** One row per tenant: annual-leave entitlement parameters and balance policy. */
@Entity
@Table(name = "leave_settings")
class LeaveSettings extends TenantEntity {

	@Column(nullable = false)
	private int firstYearDays;

	@Column(nullable = false)
	private int eligibilityMonths;

	@Column(nullable = false)
	private int standardDays;

	@Column(nullable = false)
	private int seniorDays;

	@Column(nullable = false)
	private int seniorServiceYears;

	@Column(nullable = false)
	private int seniorAge;

	@Column(nullable = false)
	private int disabilityDays;

	@Column(nullable = false)
	private boolean allowNegativeBalance;

	LeaveSettings() {
		apply(EntitlementRules.STATUTORY, false);
	}

	void apply(EntitlementRules rules, boolean allowNegativeBalance) {
		this.firstYearDays = rules.firstYearDays();
		this.eligibilityMonths = rules.eligibilityMonths();
		this.standardDays = rules.standardDays();
		this.seniorDays = rules.seniorDays();
		this.seniorServiceYears = rules.seniorServiceYears();
		this.seniorAge = rules.seniorAge();
		this.disabilityDays = rules.disabilityDays();
		this.allowNegativeBalance = allowNegativeBalance;
	}

	EntitlementRules rules() {
		return new EntitlementRules(firstYearDays, eligibilityMonths, standardDays, seniorDays, seniorServiceYears,
				seniorAge, disabilityDays);
	}

	boolean isAllowNegativeBalance() {
		return allowNegativeBalance;
	}

}
