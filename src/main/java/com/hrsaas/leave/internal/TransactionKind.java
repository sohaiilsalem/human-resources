package com.hrsaas.leave.internal;

enum TransactionKind {

	/** Yearly statutory grant; one row per employee, type and year, kept in sync with the rules. */
	ENTITLEMENT,

	/** Approved leave (negative). */
	USAGE,

	/** Cancellation of approved leave (positive). */
	REVERSAL,

	/** Manual correction by HR, e.g. opening balances or carried-over days. */
	ADJUSTMENT

}
