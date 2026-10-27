package com.hrsaas.request.internal;

import java.util.Set;

/** Who is acting. {@code employeeId} is null for users without an employee record. */
record RequestActor(Long userId, Long employeeId, Set<String> authorities) {

	boolean canManage() {
		return authorities.contains("request:manage");
	}

	boolean isEmployee(Long id) {
		return employeeId != null && employeeId.equals(id);
	}

}
