package com.hrsaas.billing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects the request with {@code 402 billing.feature-not-in-plan} unless the current tenant's subscription is in
 * good standing and its plan includes the feature. Applies to controller classes and handler methods.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresFeature {

	Feature value();

}
