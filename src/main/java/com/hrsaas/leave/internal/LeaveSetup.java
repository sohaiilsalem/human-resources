package com.hrsaas.leave.internal;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrsaas.platform.TenantDirectory;
import com.hrsaas.platform.TenantInfo;
import com.hrsaas.platform.TenantRegistered;
import com.hrsaas.shared.tenant.TenantContext;

/**
 * Seeds statutory leave settings and types inside the sign-up transaction. On start-up, gives pending requests filed
 * before approval workflows existed their approval (after the workflow module has seeded its defaults).
 */
@Component
class LeaveSetup {

	private static final Logger log = LoggerFactory.getLogger(LeaveSetup.class);

	private final LeaveSettingsRepository settings;

	private final LeaveTypeRepository types;

	private final LeaveRequestService requests;

	private final TenantDirectory tenants;

	private final TransactionTemplate transactions;

	LeaveSetup(LeaveSettingsRepository settings, LeaveTypeRepository types, LeaveRequestService requests,
			TenantDirectory tenants, TransactionTemplate transactions) {
		this.settings = settings;
		this.types = types;
		this.requests = requests;
		this.tenants = tenants;
		this.transactions = transactions;
	}

	@EventListener
	@Transactional
	void seed(TenantRegistered event) {
		settings.save(new LeaveSettings());
		Map<String, LeaveType> created = new HashMap<>();
		for (LeaveTypeRequest definition : EgyptianLeaveTypes.defaults()) {
			LeaveType deductsFrom = definition.deductsFrom() != null ? created.get(definition.deductsFrom()) : null;
			created.put(definition.code(), types.save(new LeaveType(definition, deductsFrom)));
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	void startMissingApprovals() {
		for (TenantInfo tenant : tenants.findAllActive()) {
			TenantContext.runAs(tenant.id(), () -> {
				Integer started = transactions.execute(status -> requests.startMissingApprovals());
				if (started != null && started > 0) {
					log.info("Started approvals for {} pending leave request(s) of tenant {}", started, tenant.slug());
				}
			});
		}
	}

}
