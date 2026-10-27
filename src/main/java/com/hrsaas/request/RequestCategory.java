package com.hrsaas.request;

/** Groups request types in menus and reports; behaviour comes from each type's configuration, not its category. */
public enum RequestCategory {

	/** Short absences within a working day: early departure, late arrival, temporary exit. */
	PERMISSION,
	REMOTE_WORK,
	/** Business trips and missions. */
	TRAVEL,
	/** Loans, salary advances, overtime payment reviews. */
	FINANCIAL,
	/** Training courses and certifications. */
	DEVELOPMENT,
	/** Resignations and terminations. */
	LIFECYCLE,
	OTHER

}
