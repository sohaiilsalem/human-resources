package com.hrsaas.identity.internal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.hrsaas.ApiClient;
import com.hrsaas.ApiClient.Tokens;
import com.hrsaas.IntegrationTest;

@IntegrationTest
class AuthFlowIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ApiClient api;

	@Test
	void signUpLogsTheOwnerInAndStartsATrial() throws Exception {
		String slug = ApiClient.uniqueSlug("acme");
		Tokens tokens = api.signUp(slug, "Owner@Example.com");

		mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, tokens.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("owner@example.com"))
			.andExpect(jsonPath("$.roles[0]").value("OWNER"))
			.andExpect(jsonPath("$.permissions", hasItem("payroll:run")))
			.andExpect(jsonPath("$.tenant.slug").value(slug));

		mvc.perform(get("/api/v1/billing/subscription").header(HttpHeaders.AUTHORIZATION, tokens.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("TRIAL"))
			.andExpect(jsonPath("$.plan.code").value("PRO"));
	}

	@Test
	void loginWithCorrectAndWrongPassword() throws Exception {
		String slug = ApiClient.uniqueSlug("login");
		api.signUp(slug, "hr@example.com");

		login(slug, "HR@example.com ", ApiClient.PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"));

		login(slug, "hr@example.com", "wrong-password").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("auth.invalid-credentials"));
		login("no-such-company", "hr@example.com", ApiClient.PASSWORD).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("auth.invalid-credentials"));
	}

	@Test
	void errorsAreLocalizedFromAcceptLanguage() throws Exception {
		mvc.perform(post("/api/v1/auth/login").header(HttpHeaders.ACCEPT_LANGUAGE, "ar")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"tenant": "nobody", "email": "x@example.com", "password": "whatever1"}"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail", containsString("كلمة المرور")));
	}

	@Test
	void refreshRotatesAndDetectsReuse() throws Exception {
		Tokens first = api.signUp(ApiClient.uniqueSlug("refresh"), "owner@example.com");

		Tokens second = Tokens.of(refresh(first.refreshToken()).andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString());
		mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, second.bearer())).andExpect(status().isOk());

		// replaying the rotated token revokes the whole family, including the token issued in its place
		refresh(first.refreshToken()).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("auth.invalid-refresh-token"));
		refresh(second.refreshToken()).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesTheRefreshToken() throws Exception {
		Tokens tokens = api.signUp(ApiClient.uniqueSlug("logout"), "owner@example.com");

		mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\": \"%s\"}".formatted(tokens.refreshToken())))
			.andExpect(status().isNoContent());

		refresh(tokens.refreshToken()).andExpect(status().isUnauthorized());
	}

	@Test
	void malformedRefreshTokenIsRejected() throws Exception {
		refresh("not-a-token").andExpect(status().isUnauthorized());
		refresh("00000000-0000-0000-0000-000000000000.abc").andExpect(status().isUnauthorized());
	}

	@Test
	void signUpValidatesInputAndRejectsTakenCompanyCode() throws Exception {
		mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
				{"companyName": "Bad", "tenant": "Bad Slug!", "fullName": "X", "email": "not-an-email",
				 "password": "short"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("validation"))
			.andExpect(jsonPath("$.errors[*].field", hasItem("tenant")))
			.andExpect(jsonPath("$.errors[*].field", hasItem("email")))
			.andExpect(jsonPath("$.errors[*].field", hasItem("password")));

		String slug = ApiClient.uniqueSlug("taken");
		api.signUp(slug, "first@example.com");
		mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
				{"companyName": "Other", "tenant": "%s", "fullName": "Other", "email": "second@example.com",
				 "password": "%s"}""".formatted(slug, ApiClient.PASSWORD)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("tenant.slug-taken"));
	}

	@Test
	void protectedEndpointsRequireAToken() throws Exception {
		mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value"))
			.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/billing/plans"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].code", hasItem("ENTERPRISE")));
	}

	private ResultActions login(String tenant, String email, String password) throws Exception {
		return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"tenant": "%s", "email": "%s", "password": "%s"}""".formatted(tenant, email, password)));
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\": \"%s\"}".formatted(refreshToken)));
	}

}
