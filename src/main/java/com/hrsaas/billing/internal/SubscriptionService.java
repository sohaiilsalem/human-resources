package com.hrsaas.billing.internal;

import java.time.Clock;
import java.time.Instant;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.billing.Feature;
import com.hrsaas.billing.FeatureGate;
import com.hrsaas.platform.TenantRegistered;

@Service
@Transactional(readOnly = true)
@EnableConfigurationProperties(BillingProperties.class)
class SubscriptionService implements FeatureGate {

	private final SubscriptionRepository subscriptions;

	private final PlanRepository plans;

	private final BillingProperties properties;

	private final Clock clock;

	SubscriptionService(SubscriptionRepository subscriptions, PlanRepository plans, BillingProperties properties,
			Clock clock) {
		this.subscriptions = subscriptions;
		this.plans = plans;
		this.properties = properties;
		this.clock = clock;
	}

	/** Synchronous, inside the sign-up transaction: a tenant never exists without a subscription. */
	@EventListener
	@Transactional
	void startTrial(TenantRegistered event) {
		Plan plan = plans.findByCode(properties.trialPlan())
			.orElseThrow(() -> new IllegalStateException("Trial plan not found: " + properties.trialPlan()));
		Instant now = clock.instant();
		subscriptions.save(Subscription.trial(event.tenantId(), plan, now, now.plus(properties.trialLength())));
	}

	@Override
	public boolean isEnabled(Long tenantId, Feature feature) {
		return subscriptions.findByTenantId(tenantId)
			.filter(subscription -> subscription.isInGoodStanding(clock.instant()))
			.map(subscription -> subscription.getPlan().includes(feature))
			.orElse(false);
	}

}
