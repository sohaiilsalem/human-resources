package com.hrsaas.billing;

public interface FeatureGate {

	boolean isEnabled(Long tenantId, Feature feature);

}
