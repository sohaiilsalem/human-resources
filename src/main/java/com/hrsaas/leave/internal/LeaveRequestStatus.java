package com.hrsaas.leave.internal;

import java.util.EnumSet;
import java.util.Set;

enum LeaveRequestStatus {

	PENDING, APPROVED, REJECTED, CANCELLED;

	/** Requests that hold days: they block overlaps and count against limits. */
	static final Set<LeaveRequestStatus> ACTIVE = EnumSet.of(PENDING, APPROVED);

}
