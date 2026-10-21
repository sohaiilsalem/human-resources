package com.hrsaas.leave.internal;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.platform.TenantRegistered;

/** Seeds statutory leave settings and types inside the sign-up transaction. */
@Component
class LeaveSetup {

	private final LeaveSettingsRepository settings;

	private final LeaveTypeRepository types;

	LeaveSetup(LeaveSettingsRepository settings, LeaveTypeRepository types) {
		this.settings = settings;
		this.types = types;
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

}
