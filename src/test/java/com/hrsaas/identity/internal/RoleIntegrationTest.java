package com.hrsaas.identity.internal;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.hrsaas.ApiClient;
import com.hrsaas.IntegrationTest;

@IntegrationTest
class RoleIntegrationTest {

	private static final String PASSWORD = "Passw0rd!Roles";

	@Autowired
	MockMvc mvc;

	@Autowired
	ApiClient api;

	private String slug;

	private String owner;

	@BeforeEach
	void setUpCompany() throws Exception {
		slug = ApiClient.uniqueSlug("roles");
		owner = api.signUp(slug, "owner@example.com").bearer();
	}

	@Test
	void newCompanyGetsTheDefaultRoles() throws Exception {
		call(get("/api/v1/roles"), owner, null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].code").value(
					containsInAnyOrder("OWNER", "HR_ADMIN", "PAYROLL_ADMIN", "MANAGER", "EMPLOYEE")))
			.andExpect(jsonPath("$[?(@.code=='OWNER')].permissions[*]", hasItem("billing:manage")))
			.andExpect(jsonPath("$[?(@.code=='MANAGER')].permissions[*]")
				.value(containsInAnyOrder("employee:read", "leave:approve", "self:service")));
	}

	@Test
	void customRoleGrantsExactlyItsPermissions() throws Exception {
		call(post("/api/v1/roles"), owner, """
				{"code": "RECRUITER", "nameEn": "Recruiter", "nameAr": "مسؤول التوظيف",
				 "permissions": ["employee:read", "employee:write"]}""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.owner").value(false));

		String recruiter = createUserAndLogin("recruiter@example.com", "RECRUITER");
		call(get("/api/v1/me"), recruiter, null).andExpect(status().isOk())
			.andExpect(jsonPath("$.roles[0]").value("RECRUITER"))
			.andExpect(jsonPath("$.permissions").value(containsInAnyOrder("employee:read", "employee:write")));
		call(get("/api/v1/employees"), recruiter, null).andExpect(status().isOk());
		call(get("/api/v1/users"), recruiter, null).andExpect(status().isForbidden());
	}

	@Test
	void hrAdminCannotEscalateBeyondOwnPermissions() throws Exception {
		String hr = createUserAndLogin("hr@example.com", "HR_ADMIN");

		expectError(call(post("/api/v1/roles"), hr, """
				{"code": "SUPER", "nameEn": "Super", "nameAr": "سوبر", "permissions": ["billing:manage"]}"""),
				403, "role.exceeds-own-permissions");
		expectError(call(post("/api/v1/users"), hr, userBody("payroll@example.com", "PAYROLL_ADMIN")), 403,
				"role.exceeds-own-permissions");
		expectError(call(post("/api/v1/users"), hr, userBody("boss@example.com", "OWNER")), 403,
				"user.cannot-grant-owner");
		call(post("/api/v1/users"), hr, userBody("staff@example.com", "EMPLOYEE")).andExpect(status().isCreated());
	}

	@Test
	void ownerRoleIsLockedAndRolesInUseCannotBeDeleted() throws Exception {
		expectError(call(put("/api/v1/roles/OWNER"), owner, """
				{"nameEn": "Boss", "nameAr": "رئيس", "permissions": []}"""), 403, "role.owner-immutable");
		expectError(call(delete("/api/v1/roles/OWNER"), owner, null), 403, "role.owner-immutable");

		createUserAndLogin("staff@example.com", "EMPLOYEE");
		expectError(call(delete("/api/v1/roles/EMPLOYEE"), owner, null), 409, "role.in-use");

		call(delete("/api/v1/roles/PAYROLL_ADMIN"), owner, null).andExpect(status().isNoContent());
		call(get("/api/v1/roles"), owner, null).andExpect(jsonPath("$", hasSize(4)))
			.andExpect(jsonPath("$[*].code", not(hasItem("PAYROLL_ADMIN"))));
	}

	@Test
	void unknownRolesAndPermissionsAreRejected() throws Exception {
		expectError(call(post("/api/v1/users"), owner, userBody("x@example.com", "NOPE")), 422, "role.not-found");
		expectError(call(post("/api/v1/roles"), owner, """
				{"code": "ODD", "nameEn": "Odd", "nameAr": "غريب", "permissions": ["coffee:make"]}"""), 422,
				"role.unknown-permission");
	}

	@Test
	void updatedPermissionsApplyFromTheNextToken() throws Exception {
		String manager = createUserAndLogin("manager@example.com", "MANAGER");
		call(get("/api/v1/users"), manager, null).andExpect(status().isForbidden());

		call(put("/api/v1/roles/MANAGER"), owner, """
				{"nameEn": "Team lead", "nameAr": "قائد الفريق",
				 "permissions": ["employee:read", "leave:approve", "self:service", "user:manage"]}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nameEn").value("Team lead"));

		String refreshed = login("manager@example.com");
		call(get("/api/v1/users"), refreshed, null).andExpect(status().isOk());
	}

	private String createUserAndLogin(String email, String role) throws Exception {
		call(post("/api/v1/users"), owner, userBody(email, role)).andExpect(status().isCreated());
		return login(email);
	}

	private String login(String email) throws Exception {
		String json = call(post("/api/v1/auth/login"), null, """
				{"tenant": "%s", "email": "%s", "password": "%s"}""".formatted(slug, email, PASSWORD))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ApiClient.Tokens.of(json).bearer();
	}

	private static String userBody(String email, String role) {
		return """
				{"email": "%s", "fullName": "%1$s", "password": "%s", "roles": ["%s"]}""".formatted(email, PASSWORD,
				role);
	}

	private ResultActions call(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
		if (token != null) {
			request.header(HttpHeaders.AUTHORIZATION, token);
		}
		if (body != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mvc.perform(request);
	}

	private static void expectError(ResultActions result, int status, String code) throws Exception {
		result.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
	}

}
