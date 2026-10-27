package com.hrsaas.request.internal;

import java.util.EnumSet;
import java.util.Set;

/** What the employee sees: {@code PENDING → APPROVED | REJECTED}, and {@code PENDING | APPROVED → CANCELLED}. */
enum RequestStatus {

	PENDING, APPROVED, REJECTED, CANCELLED;

	/** Requests that count against a type's monthly limit. */
	static final Set<RequestStatus> ACTIVE = EnumSet.of(PENDING, APPROVED);

}
