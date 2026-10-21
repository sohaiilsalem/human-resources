package com.hrsaas.billing.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.hrsaas.billing.FeatureGate;
import com.hrsaas.billing.RequiresFeature;
import com.hrsaas.shared.tenant.TenantContext;
import com.hrsaas.shared.web.ApiException;

class FeatureGateInterceptor implements HandlerInterceptor {

	private final FeatureGate gate;

	FeatureGateInterceptor(FeatureGate gate) {
		this.gate = gate;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod method)) {
			return true;
		}
		RequiresFeature required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresFeature.class);
		if (required == null) {
			required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresFeature.class);
		}
		if (required != null && !gate.isEnabled(TenantContext.require(), required.value())) {
			throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "billing.feature-not-in-plan", required.value());
		}
		return true;
	}

}
