package com.hrsaas.billing.internal;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("hrsaas.billing")
record BillingProperties(@DefaultValue("PRO") String trialPlan, @DefaultValue("14d") Duration trialLength) {

}
