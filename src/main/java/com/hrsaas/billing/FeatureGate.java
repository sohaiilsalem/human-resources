package com.hrsaas.billing;

import java.util.UUID;

public interface FeatureGate {

	boolean isEnabled(UUID tenantId, Feature feature);

}
