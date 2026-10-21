package com.hrsaas.leave.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrsaas.employee.EmployeeDirectory;
import com.hrsaas.platform.TenantDirectory;
import com.hrsaas.platform.TenantInfo;
import com.hrsaas.shared.tenant.TenantContext;

/**
 * Nightly: brings every active employee's annual entitlement for the current year up to date (new year, completed
 * probation, 10-year anniversary, turning 50). Balance reads and requests also sync on demand, so this keeps reports
 * correct rather than being required for correctness.
 */
@Component
class EntitlementJob {

	private static final Logger log = LoggerFactory.getLogger(EntitlementJob.class);

	private final TenantDirectory tenants;

	private final EmployeeDirectory employees;

	private final EntitlementService entitlements;

	private final TransactionTemplate tx;

	private final Clock clock;

	EntitlementJob(TenantDirectory tenants, EmployeeDirectory employees, EntitlementService entitlements,
			TransactionTemplate tx, Clock clock) {
		this.tenants = tenants;
		this.employees = employees;
		this.entitlements = entitlements;
		this.tx = tx;
		this.clock = clock;
	}

	@Scheduled(cron = "${hrsaas.leave.entitlement-cron:0 30 0 * * *}", zone = "Africa/Cairo")
	void syncAll() {
		for (TenantInfo tenant : tenants.findAllActive()) {
			try {
				syncTenant(tenant);
			}
			catch (RuntimeException ex) {
				log.error("Leave entitlement sync failed for tenant {}", tenant.slug(), ex);
			}
		}
	}

	void syncTenant(TenantInfo tenant) {
		int year = LocalDate.now(clock.withZone(ZoneId.of(tenant.timeZone()))).getYear();
		TenantContext.runAs(tenant.id(), () -> tx.executeWithoutResult(status -> {
			entitlements.annualType().ifPresent(annual -> {
				for (var employeeId : employees.findActiveIds()) {
					employees.findById(employeeId).ifPresent(employee -> entitlements.sync(employee, annual, year));
				}
			});
		}));
	}

}
