package com.hrsaas.identity.internal;

import static com.hrsaas.identity.Permission.ATTENDANCE_MANAGE;
import static com.hrsaas.identity.Permission.EMPLOYEE_READ;
import static com.hrsaas.identity.Permission.EMPLOYEE_WRITE;
import static com.hrsaas.identity.Permission.LEAVE_APPROVE;
import static com.hrsaas.identity.Permission.LEAVE_MANAGE;
import static com.hrsaas.identity.Permission.ORGANIZATION_MANAGE;
import static com.hrsaas.identity.Permission.PAYROLL_READ;
import static com.hrsaas.identity.Permission.PAYROLL_RUN;
import static com.hrsaas.identity.Permission.REQUEST_MANAGE;
import static com.hrsaas.identity.Permission.SELF_SERVICE;
import static com.hrsaas.identity.Permission.USER_MANAGE;
import static com.hrsaas.identity.Permission.WORKFLOW_MANAGE;

import java.util.List;
import java.util.Set;

/** Roles every new company starts with (ARCHITECTURE §5). Existing companies got them from V5__roles.sql
 * (and the request/workflow permissions from V7). */
final class DefaultRoles {

	private DefaultRoles() {
	}

	static List<Role> create() {
		return List.of(new Role(Role.OWNER, "Owner", "المالك", Set.of()),
				new Role("HR_ADMIN", "HR administrator", "مسؤول الموارد البشرية",
						Set.of(USER_MANAGE, ORGANIZATION_MANAGE, EMPLOYEE_READ, EMPLOYEE_WRITE, ATTENDANCE_MANAGE,
								LEAVE_MANAGE, LEAVE_APPROVE, REQUEST_MANAGE, WORKFLOW_MANAGE, SELF_SERVICE)),
				new Role("PAYROLL_ADMIN", "Payroll administrator", "مسؤول الرواتب",
						Set.of(EMPLOYEE_READ, PAYROLL_READ, PAYROLL_RUN, SELF_SERVICE)),
				new Role("MANAGER", "Manager", "مدير", Set.of(EMPLOYEE_READ, LEAVE_APPROVE, SELF_SERVICE)),
				new Role("EMPLOYEE", "Employee", "موظف", Set.of(SELF_SERVICE)));
	}

}
