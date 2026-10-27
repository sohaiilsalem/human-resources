package com.hrsaas.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.hrsaas.ApiClient;
import com.hrsaas.ApiClient.Tokens;
import com.hrsaas.IntegrationTest;
import com.hrsaas.platform.TenantDirectory;

@IntegrationTest
class FeatureGateIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ApiClient api;

	@Autowired
	TenantDirectory tenants;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void trialOnProIncludesPayroll() throws Exception {
		Tokens tokens = api.signUp(ApiClient.uniqueSlug("gate-pro"), "owner@example.com");

		mvc.perform(get("/api/v1/test/payroll-probe").header(HttpHeaders.AUTHORIZATION, tokens.bearer()))
			.andExpect(status().isOk());
	}

	@Test
	void basicPlanDoesNotIncludePayroll() throws Exception {
		String slug = ApiClient.uniqueSlug("gate-basic");
		Tokens tokens = api.signUp(slug, "owner@example.com");
		jdbc.update("update subscriptions set plan_id = (select id from plans where code = 'BASIC') where tenant_id = ?",
				tenantId(slug));

		mvc.perform(get("/api/v1/test/payroll-probe").header(HttpHeaders.AUTHORIZATION, tokens.bearer()))
			.andExpect(status().isPaymentRequired())
			.andExpect(jsonPath("$.code").value("billing.feature-not-in-plan"));
	}

	@Test
	void expiredTrialLosesFeatures() throws Exception {
		String slug = ApiClient.uniqueSlug("gate-expired");
		Tokens tokens = api.signUp(slug, "owner@example.com");
		jdbc.update("update subscriptions set trial_ends_at = now() - interval '1 day' where tenant_id = ?",
				tenantId(slug));

		mvc.perform(get("/api/v1/test/payroll-probe").header(HttpHeaders.AUTHORIZATION, tokens.bearer()))
			.andExpect(status().isPaymentRequired());
	}

	private Long tenantId(String slug) {
		return tenants.findBySlug(slug).orElseThrow().id();
	}

}
