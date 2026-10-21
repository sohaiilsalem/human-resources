package com.hrsaas.billing.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.hrsaas.billing.SubscriptionStatus;
import com.hrsaas.shared.persistence.BaseEntity;

/** Platform table (one row per tenant); {@code tenant_id} is a plain column here, not a tenant filter. */
@Entity
@Table(name = "subscriptions")
class Subscription extends BaseEntity {

	@Column(nullable = false, unique = true, updatable = false)
	private UUID tenantId;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "plan_id")
	private Plan plan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SubscriptionStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private BillingCycle billingCycle;

	@Column(nullable = false)
	private int seats;

	@Column(nullable = false)
	private Instant currentPeriodStart;

	@Column(nullable = false)
	private Instant currentPeriodEnd;

	private Instant trialEndsAt;

	protected Subscription() {
	}

	static Subscription trial(UUID tenantId, Plan plan, Instant start, Instant trialEndsAt) {
		Subscription subscription = new Subscription();
		subscription.tenantId = tenantId;
		subscription.plan = plan;
		subscription.status = SubscriptionStatus.TRIAL;
		subscription.billingCycle = BillingCycle.MONTHLY;
		subscription.seats = 0;
		subscription.currentPeriodStart = start;
		subscription.currentPeriodEnd = trialEndsAt;
		subscription.trialEndsAt = trialEndsAt;
		return subscription;
	}

	/** Whether paid features are usable now. A lapsed trial counts as not in good standing. */
	boolean isInGoodStanding(Instant now) {
		return switch (status) {
			case TRIAL -> trialEndsAt != null && now.isBefore(trialEndsAt);
			case ACTIVE, PAST_DUE -> true;
			case SUSPENDED, CANCELLED -> false;
		};
	}

	UUID getTenantId() {
		return tenantId;
	}

	Plan getPlan() {
		return plan;
	}

	SubscriptionStatus getStatus() {
		return status;
	}

	BillingCycle getBillingCycle() {
		return billingCycle;
	}

	int getSeats() {
		return seats;
	}

	Instant getCurrentPeriodStart() {
		return currentPeriodStart;
	}

	Instant getCurrentPeriodEnd() {
		return currentPeriodEnd;
	}

	Instant getTrialEndsAt() {
		return trialEndsAt;
	}

}
