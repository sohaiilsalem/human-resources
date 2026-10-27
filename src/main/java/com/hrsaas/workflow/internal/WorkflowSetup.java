package com.hrsaas.workflow.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrsaas.platform.TenantDirectory;
import com.hrsaas.platform.TenantInfo;
import com.hrsaas.platform.TenantRegistered;
import com.hrsaas.shared.tenant.TenantContext;

/**
 * Seeds the default workflows inside the sign-up transaction, and on start-up for companies that have none yet (those
 * created before workflows existed). Idempotent.
 */
@Component
class WorkflowSetup {

	private static final Logger log = LoggerFactory.getLogger(WorkflowSetup.class);

	private final ApprovalWorkflowRepository workflows;

	private final TenantDirectory tenants;

	private final TransactionTemplate transactions;

	WorkflowSetup(ApprovalWorkflowRepository workflows, TenantDirectory tenants, TransactionTemplate transactions) {
		this.workflows = workflows;
		this.tenants = tenants;
		this.transactions = transactions;
	}

	@EventListener
	@Transactional
	void seed(TenantRegistered event) {
		workflows.saveAll(DefaultWorkflows.create());
	}

	/** Runs before the other modules' start-up backfills, which may start approvals. */
	@EventListener(ApplicationReadyEvent.class)
	@Order(Ordered.HIGHEST_PRECEDENCE)
	void backfill() {
		for (TenantInfo tenant : tenants.findAllActive()) {
			TenantContext.runAs(tenant.id(), () -> transactions.executeWithoutResult(status -> {
				if (workflows.count() == 0) {
					workflows.saveAll(DefaultWorkflows.create());
					log.info("Seeded default approval workflows for tenant {}", tenant.slug());
				}
			}));
		}
	}

}
