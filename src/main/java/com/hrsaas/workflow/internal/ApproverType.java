package com.hrsaas.workflow.internal;

/** How a step finds its approver. Job titles are deliberately not an option: they describe work, not authority. */
enum ApproverType {

	/** The employee's manager ({@code employees.manager_id}). */
	DIRECT_MANAGER,

	/** The manager {@code managerLevel} levels up: 2 = the manager's manager. */
	MANAGER_LEVEL,

	/** Any active user holding the role; the first of them to decide completes the step. */
	ROLE,

	/** One named user. */
	USER

}
