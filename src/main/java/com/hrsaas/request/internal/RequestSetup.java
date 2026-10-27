package com.hrsaas.request.internal;

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
 * Seeds the default request types inside the sign-up transaction, and on start-up for companies that have none yet
 * (those created before request types existed). Idempotent.
 */
@Component
class RequestSetup {

	private static final Logger log = LoggerFactory.getLogger(RequestSetup.class);

	private final RequestTypeRepository types;

	private final TenantDirectory tenants;

	private final TransactionTemplate transactions;

	RequestSetup(RequestTypeRepository types, TenantDirectory tenants, TransactionTemplate transactions) {
		this.types = types;
		this.tenants = tenants;
		this.transactions = transactions;
	}

	@EventListener
	@Transactional
	void seed(TenantRegistered event) {
		seed();
	}

	@EventListener(ApplicationReadyEvent.class)
	void backfill() {
		for (TenantInfo tenant : tenants.findAllActive()) {
			TenantContext.runAs(tenant.id(), () -> transactions.executeWithoutResult(status -> {
				if (types.count() == 0) {
					seed();
					log.info("Seeded default request types for tenant {}", tenant.slug());
				}
			}));
		}
	}

	private void seed() {
		types.saveAll(DefaultRequestTypes.defaults().stream().map(RequestType::new).toList());
	}

}
