package com.hrsaas.identity;

import static com.hrsaas.identity.Permission.*;

import java.util.EnumSet;
import java.util.Set;

/** Tenant roles (ARCHITECTURE §5). Managers are additionally scoped to their reporting line in services. */
public enum Role {

	OWNER(EnumSet.allOf(Permission.class)),
	HR_ADMIN(EnumSet.of(USER_MANAGE, ORGANIZATION_MANAGE, EMPLOYEE_READ, EMPLOYEE_WRITE, ATTENDANCE_MANAGE,
			LEAVE_MANAGE, LEAVE_APPROVE, SELF_SERVICE)),
	PAYROLL_ADMIN(EnumSet.of(EMPLOYEE_READ, PAYROLL_READ, PAYROLL_RUN, SELF_SERVICE)),
	MANAGER(EnumSet.of(EMPLOYEE_READ, LEAVE_APPROVE, SELF_SERVICE)),
	EMPLOYEE(EnumSet.of(SELF_SERVICE));

	private final Set<Permission> permissions;

	Role(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	public Set<Permission> permissions() {
		return permissions;
	}

}
