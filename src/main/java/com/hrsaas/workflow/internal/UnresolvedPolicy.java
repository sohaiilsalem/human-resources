package com.hrsaas.workflow.internal;

/**
 * What a step does when its approver is missing: no manager at that level, the manager has left or has no active
 * account, nobody else holds the role, or the only candidate is the employee themselves.
 */
enum UnresolvedPolicy {

	/** The step is skipped and the approval moves on. */
	SKIP,

	/** Manager steps go further up the reporting line; when it runs out, and for other steps, the fallback role. */
	ESCALATE,

	/** Straight to the fallback role. */
	FALLBACK

}
