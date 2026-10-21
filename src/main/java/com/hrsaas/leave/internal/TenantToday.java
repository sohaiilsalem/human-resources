package com.hrsaas.leave.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.hrsaas.platform.TenantDirectory;
import com.hrsaas.platform.TenantInfo;
import com.hrsaas.shared.tenant.TenantContext;

/** "Today" in the current tenant's time zone (Africa/Cairo by default). */
@Component
class TenantToday {

	private final TenantDirectory tenants;

	private final Clock clock;

	TenantToday(TenantDirectory tenants, Clock clock) {
		this.tenants = tenants;
		this.clock = clock;
	}

	LocalDate get() {
		ZoneId zone = tenants.findById(TenantContext.require())
			.map(TenantInfo::timeZone)
			.map(ZoneId::of)
			.orElse(ZoneId.of("Africa/Cairo"));
		return LocalDate.now(clock.withZone(zone));
	}

}
