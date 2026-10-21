package com.hrsaas.leave.internal;

import java.util.Set;
import java.util.UUID;

/** Who is acting, as far as leave rules care. {@code employeeId} is null for users without an employee record. */
record Actor(UUID userId, UUID employeeId, Set<String> authorities) {

	boolean canManage() {
		return authorities.contains("leave:manage");
	}

	boolean canApprove() {
		return authorities.contains("leave:approve");
	}

	boolean isEmployee(UUID id) {
		return employeeId != null && employeeId.equals(id);
	}

}
