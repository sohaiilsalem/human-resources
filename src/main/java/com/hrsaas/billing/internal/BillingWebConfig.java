package com.hrsaas.billing.internal;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hrsaas.billing.FeatureGate;

@Configuration(proxyBeanMethods = false)
class BillingWebConfig implements WebMvcConfigurer {

	private final FeatureGate gate;

	BillingWebConfig(FeatureGate gate) {
		this.gate = gate;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new FeatureGateInterceptor(gate)).addPathPatterns("/api/**");
	}

}
