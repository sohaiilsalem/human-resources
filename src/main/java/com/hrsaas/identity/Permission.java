package com.hrsaas.identity;

/** Fine-grained authorities checked with {@code @PreAuthorize("hasAuthority('payroll:run')")}. */
public enum Permission {

	TENANT_MANAGE("tenant:manage"),
	USER_MANAGE("user:manage"),
	BILLING_MANAGE("billing:manage"),
	ORGANIZATION_MANAGE("organization:manage"),
	EMPLOYEE_READ("employee:read"),
	EMPLOYEE_WRITE("employee:write"),
	ATTENDANCE_MANAGE("attendance:manage"),
	LEAVE_MANAGE("leave:manage"),
	LEAVE_APPROVE("leave:approve"),
	PAYROLL_READ("payroll:read"),
	PAYROLL_RUN("payroll:run"),
	SELF_SERVICE("self:service");

	private final String authority;

	Permission(String authority) {
		this.authority = authority;
	}

	public String authority() {
		return authority;
	}

}
