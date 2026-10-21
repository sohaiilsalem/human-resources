package com.hrsaas.billing;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint guarded by {@link RequiresFeature}, used by {@link FeatureGateIntegrationTest}. */
@TestComponent
@RestController
public class PayrollProbeController {

	@GetMapping("/api/v1/test/payroll-probe")
	@RequiresFeature(Feature.PAYROLL)
	String probe() {
		return "ok";
	}

}
