package com.hrsaas.billing.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.billing.Feature;
import com.hrsaas.billing.SubscriptionStatus;
import com.hrsaas.shared.tenant.TenantContext;
import com.hrsaas.shared.web.ApiException;

@RestController
@RequestMapping("/api/v1/billing")
class BillingController {

	private final PlanRepository plans;

	private final SubscriptionRepository subscriptions;

	BillingController(PlanRepository plans, SubscriptionRepository subscriptions) {
		this.plans = plans;
		this.subscriptions = subscriptions;
	}

	/** Public: pricing page / sign-up wizard. */
	@GetMapping("/plans")
	List<PlanResponse> plans() {
		return plans.findByActiveTrueOrderBySortOrder().stream().map(PlanResponse::of).toList();
	}

	@GetMapping("/subscription")
	@PreAuthorize("hasAuthority('billing:manage')")
	SubscriptionResponse subscription() {
		Subscription subscription = subscriptions.findByTenantId(TenantContext.require())
			.orElseThrow(() -> ApiException.notFound("billing.subscription-not-found"));
		return new SubscriptionResponse(PlanResponse.of(subscription.getPlan()), subscription.getStatus(),
				subscription.getBillingCycle().name(), subscription.getSeats(), subscription.getCurrentPeriodStart(),
				subscription.getCurrentPeriodEnd(), subscription.getTrialEndsAt());
	}

	record PlanResponse(String code, String nameEn, String nameAr, BigDecimal monthlyPricePerEmployee,
			BigDecimal yearlyPricePerEmployee, Integer maxEmployees, Integer maxBranches, Set<Feature> features) {

		static PlanResponse of(Plan plan) {
			return new PlanResponse(plan.getCode(), plan.getNameEn(), plan.getNameAr(),
					plan.getMonthlyPricePerEmployee(), plan.getYearlyPricePerEmployee(), plan.getMaxEmployees(),
					plan.getMaxBranches(), plan.getFeatures());
		}

	}

	record SubscriptionResponse(PlanResponse plan, SubscriptionStatus status, String billingCycle, int seats,
			Instant currentPeriodStart, Instant currentPeriodEnd, Instant trialEndsAt) {
	}

}
