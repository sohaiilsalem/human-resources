package com.hrsaas.leave.internal;

import java.util.Set;

/** Who is acting, as far as leave rules care. {@code employeeId} is null for users without an employee record. */
record Actor(Long userId, Long employeeId, Set<String> authorities) {

	boolean canManage() {
		return authorities.contains("leave:manage");
	}

	boolean canApprove() {
		return authorities.contains("leave:approve");
	}

	boolean isEmployee(Long id) {
		return employeeId != null && employeeId.equals(id);
	}

}
