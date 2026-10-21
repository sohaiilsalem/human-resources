package com.hrsaas.leave.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.hrsaas.ApiClient;
import com.hrsaas.ApiClient.Tokens;
import com.hrsaas.IntegrationTest;

/**
 * End-to-end leave flows for one company: HR (the owner), a manager and two team members, plus an employee outside
 * the manager's team. Dates are relative to today; the calendar uses the default Friday/Saturday weekend.
 */
@IntegrationTest
class LeaveIntegrationTest {

	private static final String PASSWORD = ApiClient.PASSWORD;

	@Autowired
	MockMvc mvc;

	@Autowired
	ApiClient api;

	private final LocalDate today = LocalDate.now(ZoneId.of("Africa/Cairo"));

	/** A Monday at least three weeks ahead. */
	private final LocalDate monday = today.plusDays(21).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

	private String slug;

	private String hr;

	private String managerId;

	private String staffId;

	private String outsiderId;

	private String newcomerId;

	private String manager;

	private String staff;

	@BeforeEach
	void setUpCompany() throws Exception {
		slug = ApiClient.uniqueSlug("leave");
		hr = api.signUp(slug, "hr@example.com").bearer();

		managerId = hire("M1", "MALE", "1985-03-01", today.minusYears(5), null);
		staffId = hire("S1", "FEMALE", "1995-07-15", today.minusYears(2), managerId);
		outsiderId = hire("O1", "MALE", "1992-01-20", today.minusYears(3), null);
		newcomerId = hire("N1", "MALE", "2000-05-05", today.minusMonths(2), managerId);

		createUser("manager@example.com", "MANAGER", managerId);
		createUser("staff@example.com", "EMPLOYEE", staffId);
		manager = login("manager@example.com");
		staff = login("staff@example.com");
	}

	@Test
	void newCompanyHasEgyptianLeaveTypes() throws Exception {
		List<String> codes = JsonPath.read(call(get("/api/v1/leave/types"), staff, 200), "$[*].code");
		assertThat(codes).containsExactlyInAnyOrder("ANNUAL", "CASUAL", "SICK", "MATERNITY", "HAJJ", "UNPAID");
	}

	@Test
	void balancesShowTheStatutoryEntitlement() throws Exception {
		String balances = call(get("/api/v1/me/leave/balances"), staff, 200);
		assertThat(number(balances, "$[?(@.code=='ANNUAL')].entitledThisYear")).isEqualTo(21);
		assertThat(number(balances, "$[?(@.code=='ANNUAL')].available")).isEqualTo(21);
		assertThat(JsonPath.<List<String>>read(balances, "$[?(@.code=='CASUAL')].deductsFrom")).containsExactly("ANNUAL");

		String fifty = hire("F1", "MALE", today.minusYears(51).toString(), today.minusYears(2), null);
		String hrView = call(get("/api/v1/leave/employees/" + fifty + "/balances"), hr, 200);
		assertThat(number(hrView, "$[?(@.code=='ANNUAL')].entitledThisYear")).isEqualTo(30);
	}

	@Test
	void requestApproveAndCancelMoveTheBalance() throws Exception {
		LocalDate wednesday = monday.plusDays(2);
		call(post("/api/v1/organization/holidays"), hr, 201, Map.of("startDate", wednesday.toString(), "nameEn",
				"Company holiday", "nameAr", "عطلة الشركة"));

		// Mon..next Mon = 8 days: minus Friday, Saturday and the Wednesday holiday = 5 working days
		String request = call(post("/api/v1/me/leave/requests"), staff, 201,
				leave("ANNUAL", monday, monday.plusDays(7)));
		String id = JsonPath.read(request, "$.id");
		assertThat(number(request, "$.days")).isEqualTo(5);
		assertThat(JsonPath.<String>read(request, "$.status")).isEqualTo("PENDING");
		assertBalance(staff, 21, 5, 16);

		String approved = call(post("/api/v1/leave/requests/" + id + "/approve"), manager, 200, Map.of("note", "Enjoy"));
		assertThat(JsonPath.<String>read(approved, "$.status")).isEqualTo("APPROVED");
		assertBalance(staff, 16, 0, 16);

		String cancelled = call(post("/api/v1/leave/requests/" + id + "/cancel"), hr, 200, Map.of());
		assertThat(JsonPath.<String>read(cancelled, "$.status")).isEqualTo("CANCELLED");
		assertBalance(staff, 21, 0, 21);

		List<String> kinds = JsonPath.read(call(get("/api/v1/leave/employees/" + staffId + "/transactions"), hr, 200),
				"$[*].kind");
		assertThat(kinds).containsExactlyInAnyOrder("ENTITLEMENT", "USAGE", "REVERSAL");
	}

	@Test
	void approvalIsLimitedToTheManagersTeamAndNeverOnesOwn() throws Exception {
		String outsiderLeave = JsonPath.read(call(post("/api/v1/leave/requests"), hr, 201,
				onBehalf(outsiderId, "ANNUAL", monday, monday)), "$.id");
		assertError(call(post("/api/v1/leave/requests/" + outsiderLeave + "/approve"), manager, 403, Map.of()),
				"leave.not-your-team");

		String own = JsonPath.read(call(post("/api/v1/me/leave/requests"), manager, 201, leave("ANNUAL", monday, monday)),
				"$.id");
		assertError(call(post("/api/v1/leave/requests/" + own + "/approve"), manager, 403, Map.of()),
				"leave.cannot-decide-own");

		// a plain employee cannot use the approval endpoints at all
		call(post("/api/v1/leave/requests/" + own + "/approve"), staff, 403, Map.of());

		List<String> teamRequests = JsonPath.read(call(get("/api/v1/leave/requests"), manager, 200),
				"$.content[*].employeeId");
		assertThat(teamRequests).doesNotContain(outsiderId, managerId);
	}

	@Test
	void statutoryRulesAreEnforced() throws Exception {
		// casual leave: at most 2 days at a time, and it is taken from the annual balance
		assertError(call(post("/api/v1/me/leave/requests"), staff, 422, leave("CASUAL", monday, monday.plusDays(2))),
				"leave.max-days-per-request");
		call(post("/api/v1/me/leave/requests"), staff, 201, leave("CASUAL", monday, monday.plusDays(1)));
		assertBalance(staff, 21, 2, 19);

		// overlapping leave
		assertError(call(post("/api/v1/me/leave/requests"), staff, 409, leave("ANNUAL", monday.plusDays(1),
				monday.plusDays(3))), "leave.overlap");

		// more than the balance
		LocalDate later = monday.plusWeeks(4);
		assertError(call(post("/api/v1/me/leave/requests"), staff, 422, leave("ANNUAL", later, later.plusWeeks(6))),
				"leave.insufficient-balance");

		// maternity leave only for women
		assertError(call(post("/api/v1/leave/requests"), hr, 422, onBehalf(managerId, "MATERNITY", later, later)),
				"leave.gender-restricted");

		// annual leave only after six months of service; unpaid leave is still possible
		assertError(call(post("/api/v1/leave/requests"), hr, 422, onBehalf(newcomerId, "ANNUAL", later, later)),
				"leave.not-eligible-yet");
		call(post("/api/v1/leave/requests"), hr, 201, onBehalf(newcomerId, "UNPAID", later, later));

		// Hajj leave needs five years of service
		assertError(call(post("/api/v1/leave/requests"), hr, 422, onBehalf(outsiderId, "HAJJ", later, later.plusDays(20))),
				"leave.min-service");

		// a weekend only
		LocalDate friday = monday.minusDays(3);
		assertError(call(post("/api/v1/me/leave/requests"), staff, 422, leave("ANNUAL", friday, friday.plusDays(1))),
				"leave.no-working-days");
	}

	@Test
	void employeesCanWithdrawPendingRequests() throws Exception {
		String id = JsonPath.read(call(post("/api/v1/me/leave/requests"), staff, 201, leave("ANNUAL", monday, monday)),
				"$.id");
		String cancelled = call(post("/api/v1/me/leave/requests/" + id + "/cancel"), staff, 200, Map.of());
		assertThat(JsonPath.<String>read(cancelled, "$.status")).isEqualTo("CANCELLED");
		assertBalance(staff, 21, 0, 21);
	}

	@Test
	void hrCanAdjustBalancesButNotGoBelowTheLaw() throws Exception {
		call(post("/api/v1/leave/adjustments"), hr, 201,
				Map.of("employeeId", staffId, "leaveType", "ANNUAL", "days", 4, "note", "Carried over from last year"));
		assertBalance(staff, 25, 0, 25);

		Map<String, Object> lessThanLaw = Map.of("firstYearDays", 15, "eligibilityMonths", 6, "standardDays", 18,
				"seniorDays", 30, "seniorServiceYears", 10, "seniorAge", 50, "disabilityDays", 45,
				"allowNegativeBalance", false);
		assertError(call(put("/api/v1/leave/settings"), hr, 422, lessThanLaw), "leave.settings-below-statutory");

		call(put("/api/v1/leave/settings"), staff, 403, lessThanLaw);
	}

	// ------------------------------------------------------------------ helpers

	private void assertBalance(String token, int balance, int pending, int available) throws Exception {
		String json = call(get("/api/v1/me/leave/balances"), token, 200);
		assertThat(number(json, "$[?(@.code=='ANNUAL')].balance")).as("balance").isEqualTo(balance);
		assertThat(number(json, "$[?(@.code=='ANNUAL')].pending")).as("pending").isEqualTo(pending);
		assertThat(number(json, "$[?(@.code=='ANNUAL')].available")).as("available").isEqualTo(available);
	}

	private static Map<String, Object> leave(String type, LocalDate start, LocalDate end) {
		return Map.of("leaveType", type, "startDate", start.toString(), "endDate", end.toString());
	}

	private static Map<String, Object> onBehalf(String employeeId, String type, LocalDate start, LocalDate end) {
		return Map.of("employeeId", employeeId, "leaveType", type, "startDate", start.toString(), "endDate",
				end.toString());
	}

	private String hire(String code, String gender, String birthDate, LocalDate hireDate, String managerId)
			throws Exception {
		String manager = managerId == null ? "null" : "\"" + managerId + "\"";
		String body = """
				{"code": "%s", "fullNameEn": "Employee %1$s", "fullNameAr": "موظف %1$s", "gender": "%s",
				 "birthDate": "%s", "hireDate": "%s", "managerId": %s}""".formatted(code, gender, birthDate, hireDate,
				manager);
		return JsonPath.read(perform(post("/api/v1/employees"), hr, body, 201), "$.id");
	}

	private void createUser(String email, String role, String employeeId) throws Exception {
		call(post("/api/v1/users"), hr, 201, Map.of("email", email, "fullName", email, "password", PASSWORD, "roles",
				List.of(role), "employeeId", employeeId));
	}

	private String login(String email) throws Exception {
		String json = perform(post("/api/v1/auth/login"), null, """
				{"tenant": "%s", "email": "%s", "password": "%s"}""".formatted(slug, email, PASSWORD), 200);
		return Tokens.of(json).bearer();
	}

	private String call(MockHttpServletRequestBuilder request, String token, int status) throws Exception {
		return perform(request, token, null, status);
	}

	private String call(MockHttpServletRequestBuilder request, String token, int status, Map<String, ?> body)
			throws Exception {
		return perform(request, token, toJson(body), status);
	}

	private String perform(MockHttpServletRequestBuilder request, String token, String body, int expectedStatus)
			throws Exception {
		if (token != null) {
			request.header(HttpHeaders.AUTHORIZATION, token);
		}
		if (body != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn().getResponse()
			.getContentAsString();
	}

	private static void assertError(String problem, String code) {
		assertThat(JsonPath.<String>read(problem, "$.code")).isEqualTo(code);
	}

	/** A single number; filter paths ({@code $[?(...)]}) return a one-element list. */
	private static double number(String json, String path) {
		Object value = JsonPath.read(json, path);
		if (value instanceof List<?> values) {
			assertThat(values).as(path).hasSize(1);
			value = values.getFirst();
		}
		return ((Number) value).doubleValue();
	}

	private static String toJson(Map<String, ?> body) {
		StringBuilder json = new StringBuilder("{");
		body.forEach((key, value) -> {
			if (json.length() > 1) {
				json.append(',');
			}
			json.append('"').append(key).append("\":").append(jsonValue(value));
		});
		return json.append('}').toString();
	}

	private static String jsonValue(Object value) {
		if (value instanceof Number || value instanceof Boolean) {
			return value.toString();
		}
		if (value instanceof List<?> list) {
			return list.stream().map(LeaveIntegrationTest::jsonValue).reduce((a, b) -> a + "," + b)
				.map(items -> "[" + items + "]")
				.orElse("[]");
		}
		return "\"" + value + "\"";
	}

}
