package com.hrsaas.leave.internal;

import java.util.Optional;

/**
 * Annual-leave entitlement parameters. {@link #STATUTORY} holds the minimums of Labour Law No. 14 of 2025; a tenant
 * may be more generous, never less.
 *
 * <p>
 * ⚠️ Values to be confirmed by an Egyptian labour-law advisor before go-live (ARCHITECTURE §7, §13.1).
 */
record EntitlementRules(int firstYearDays, int eligibilityMonths, int standardDays, int seniorDays,
		int seniorServiceYears, int seniorAge, int disabilityDays) {

	/** 15 days after 6 months; 21 after a year; 30 after 10 years or from age 50; persons with disabilities 45. */
	static final EntitlementRules STATUTORY = new EntitlementRules(15, 6, 21, 30, 10, 50, 45);

	/** @return the first parameter that is less favourable than the law, if any */
	Optional<String> belowStatutory() {
		if (firstYearDays < STATUTORY.firstYearDays) {
			return Optional.of("firstYearDays");
		}
		if (eligibilityMonths > STATUTORY.eligibilityMonths) {
			return Optional.of("eligibilityMonths");
		}
		if (standardDays < STATUTORY.standardDays) {
			return Optional.of("standardDays");
		}
		if (seniorDays < STATUTORY.seniorDays) {
			return Optional.of("seniorDays");
		}
		if (seniorServiceYears > STATUTORY.seniorServiceYears) {
			return Optional.of("seniorServiceYears");
		}
		if (seniorAge > STATUTORY.seniorAge) {
			return Optional.of("seniorAge");
		}
		return Optional.empty();
	}

}
