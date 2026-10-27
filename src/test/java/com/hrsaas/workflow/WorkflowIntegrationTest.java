package com.hrsaas.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.hrsaas.ApiClient;
import com.hrsaas.ApiClient.Tokens;
import com.hrsaas.IntegrationTest;

/**
 * Approval workflows end to end, through employee requests and leave. The company:
 *
 * <pre>
 * director ─┬─ manager ─┬─ staff
 *           │           └─ colleague
 *           └─ (lone: no manager)        hr (HR_ADMIN), payroll (PAYROLL_ADMIN), owner (no employee record)
 * </pre>
 */
@IntegrationTest
class WorkflowIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	MockMvc mvc;

	@Autowired
	ApiClient api;

	@Autowired
	JdbcTemplate jdbc;

	private final LocalDate today = LocalDate.now(ZoneId.of("Africa/Cairo"));

	private String slug;

	private String owner;

	private long directorId;

	private long managerId;

	private long staffId;

	private long loneId;

	private long hrEmployeeId;

	private String director;

	private String manager;

	private String staff;

	private String colleague;

	private String lone;

	private String hr;

	private String payroll;

	@BeforeEach
	void setUpCompany() throws Exception {
		slug = ApiClient.uniqueSlug("wf");
		owner = api.signUp(slug, "owner@example.com").bearer();

		directorId = hire("D1", null);
		managerId = hire("M1", directorId);
		staffId = hire("S1", managerId);
		long colleagueId = hire("S2", managerId);
		loneId = hire("L1", null);
		hrEmployeeId = hire("H1", null);
		long payrollEmployeeId = hire("P1", null);

		director = user("director@example.com", "MANAGER", directorId);
		manager = user("manager@example.com", "MANAGER", managerId);
		staff = user("staff@example.com", "EMPLOYEE", staffId);
		colleague = user("colleague@example.com", "EMPLOYEE", colleagueId);
		lone = user("lone@example.com", "EMPLOYEE", loneId);
		hr = user("hr@example.com", "HR_ADMIN", hrEmployeeId);
		payroll = user("payroll@example.com", "PAYROLL_ADMIN", payrollEmployeeId);
	}

	@Test
	void newCompanyHasDefaultWorkflowsAndRequestTypes() throws Exception {
		List<String> workflows = JsonPath.read(call(get("/api/v1/workflows"), hr, 200), "$[*].code");
		assertThat(workflows).contains("MANAGER", "MANAGER_HR", "MANAGERS_HR", "FINANCE", "HR_OWNER");

		List<String> types = JsonPath.read(call(get("/api/v1/request-types"), staff, 200), "$[*].code");
		assertThat(types).contains("EARLY_DEPARTURE", "LATE_ARRIVAL", "TEMPORARY_EXIT", "WORK_FROM_HOME", "MISSION",
				"LOAN", "SALARY_ADVANCE", "OVERTIME_REVIEW", "TRAINING_COURSE", "CERTIFICATION", "RESIGNATION",
				"TERMINATION");
	}

	@Test
	void stagesRunInOrderAndOnlyTheCurrentApproverCanAct() throws Exception {
		// RESIGNATION → MANAGERS_HR: direct manager, second-level manager, HR
		long id = id(file(staff, "RESIGNATION", Map.of("lastWorkingDay", today.plusMonths(1).toString())));
		long approval = approvalOf(id);

		// the director is the next approver, not this one; a colleague never is
		assertError(decide(approval, director, "approve", null, 403), "approval.not-eligible");
		assertError(decide(approval, colleague, "approve", null, 403), "approval.not-eligible");
		assertError(decide(approval, staff, "approve", null, 403), "approval.cannot-decide-own");

		assertThat(inboxSubjects(manager)).contains(id);
		assertThat(inboxSubjects(director)).doesNotContain(id);

		String afterManager = decide(approval, manager, "approve", "Fine by me", 200);
		assertThat(JsonPath.<String>read(afterManager, "$.status")).isEqualTo("IN_PROGRESS");
		assertThat(JsonPath.<Integer>read(afterManager, "$.currentStageNumber")).isEqualTo(2);
		assertThat(JsonPath.<Integer>read(afterManager, "$.stageCount")).isEqualTo(3);
		assertThat(inboxSubjects(director)).contains(id);

		decide(approval, director, "approve", null, 200);
		String done = decide(approval, hr, "approve", null, 200);
		assertThat(JsonPath.<String>read(done, "$.status")).isEqualTo("APPROVED");
		assertThat(JsonPath.<List<String>>read(done, "$.steps[*].status")).containsOnly("APPROVED");
		assertThat(JsonPath.<List<String>>read(done, "$.history[*].action")).startsWith("SUBMITTED")
			.contains("APPROVED")
			.endsWith("COMPLETED");

		String request = call(get("/api/v1/requests/" + id), staff, 200);
		assertThat(JsonPath.<String>read(request, "$.status")).isEqualTo("APPROVED");
	}

	@Test
	void rejectionNeedsAReasonAndEndsTheRequest() throws Exception {
		long id = id(file(staff, "MISSION", Map.of("destination", "Alexandria", "startDate", today.toString(),
				"endDate", today.plusDays(2).toString())));
		long approval = approvalOf(id);

		assertError(decide(approval, manager, "reject", null, 422), "approval.reason-required");
		String rejected = decide(approval, manager, "reject", "Budget is frozen this quarter", 200);
		assertThat(JsonPath.<String>read(rejected, "$.status")).isEqualTo("REJECTED");
		assertThat(JsonPath.<List<String>>read(rejected, "$.steps[*].status")).containsExactly("REJECTED",
				"CANCELLED");

		String request = call(get("/api/v1/requests/" + id), staff, 200);
		assertThat(JsonPath.<String>read(request, "$.status")).isEqualTo("REJECTED");
		assertThat(JsonPath.<String>read(request, "$.decisionNote")).isEqualTo("Budget is frozen this quarter");
		assertError(decide(approval, hr, "approve", null, 409), "approval.not-pending");
	}

	@Test
	void conditionsAddStepsAndParallelStepsAllHaveToApprove() throws Exception {
		// FINANCE: manager; second-level manager only above 20,000; then HR and payroll in parallel
		long big = approvalOf(id(file(staff, "LOAN", Map.of("amount", 30000, "installments", 12))));
		assertThat(JsonPath.<Integer>read(call(get("/api/v1/approvals/" + big), staff, 200), "$.stageCount"))
			.isEqualTo(3);

		long small = approvalOf(id(file(colleague, "LOAN", Map.of("amount", 5000, "installments", 5))));
		String view = call(get("/api/v1/approvals/" + small), colleague, 200);
		assertThat(JsonPath.<Integer>read(view, "$.stageCount")).isEqualTo(2);

		decide(small, manager, "approve", null, 200);
		String afterHr = decide(small, hr, "approve", null, 200);
		assertThat(JsonPath.<String>read(afterHr, "$.status")).as("payroll has not decided yet")
			.isEqualTo("IN_PROGRESS");
		String done = decide(small, payroll, "approve", null, 200);
		assertThat(JsonPath.<String>read(done, "$.status")).isEqualTo("APPROVED");
	}

	@Test
	void missingReviewersAreEscalatedOrSkipped() throws Exception {
		// no manager at all: the direct-manager step escalates to HR
		long wfh = approvalOf(id(file(lone, "WORK_FROM_HOME", Map.of("startDate", today.toString(), "endDate",
				today.toString()))));
		String view = call(get("/api/v1/approvals/" + wfh), lone, 200);
		assertThat(JsonPath.<String>read(view, "$.steps[0].assigneeRole")).isEqualTo("HR_ADMIN");
		assertThat(JsonPath.<List<String>>read(view, "$.history[*].action")).contains("ESCALATED");
		assertThat(inboxSubjects(hr)).contains(JsonPath.<Number>read(view, "$.subjectId").longValue());

		// the manager's resignation: the director approves; there is no manager above the director, so the
		// second-level step is skipped and HR is next
		long resignation = approvalOf(id(file(manager, "RESIGNATION", Map.of("lastWorkingDay",
				today.plusMonths(1).toString()))));
		decide(resignation, director, "approve", null, 200);
		String afterDirector = call(get("/api/v1/approvals/" + resignation), manager, 200);
		assertThat(JsonPath.<List<String>>read(afterDirector, "$.steps[*].status")).containsExactly("APPROVED",
				"SKIPPED", "PENDING");
	}

	@Test
	void hrNeverApprovesTheirOwnRequestEvenWithOverrideRights() throws Exception {
		// HR's own mission (MANAGER_HR): HR has no manager and is the only HR_ADMIN, so both steps go to the owner
		long approval = approvalOf(id(file(hr, "MISSION", Map.of("destination", "Cairo", "startDate",
				today.toString(), "endDate", today.toString()))));
		assertError(decide(approval, hr, "approve", null, 403), "approval.cannot-decide-own");

		decide(approval, owner, "approve", null, 200);
		String done = decide(approval, owner, "approve", null, 200);
		assertThat(JsonPath.<String>read(done, "$.status")).isEqualTo("APPROVED");
		assertThat(JsonPath.<List<String>>read(done, "$.steps[*].assigneeRole")).containsExactly("OWNER", "OWNER");
		assertThat(JsonPath.<List<String>>read(done, "$.history[*].action")).doesNotContain("OVERRIDE_APPROVED");
	}

	@Test
	void workflowAdministratorsCanOverrideAndItIsRecorded() throws Exception {
		long approval = approvalOf(id(file(staff, "WORK_FROM_HOME", Map.of("startDate", today.toString(),
				"endDate", today.toString()))));
		// HR is not the approver (the manager is) but holds workflow:manage
		String done = decide(approval, hr, "approve", "Manager unreachable, approved per policy", 200);
		assertThat(JsonPath.<String>read(done, "$.status")).isEqualTo("APPROVED");
		assertThat(JsonPath.<List<String>>read(done, "$.history[*].action")).contains("OVERRIDE_APPROVED");
		assertThat(JsonPath.<Number>read(done, "$.steps[0].decidedBy").longValue()).isEqualTo(userIdOf(hr));
	}

	@Test
	void reassignedStepsMoveToTheNewApprover() throws Exception {
		long approval = approvalOf(id(file(staff, "WORK_FROM_HOME", Map.of("startDate", today.toString(),
				"endDate", today.toString()))));
		long stepId = JsonPath.<Number>read(call(get("/api/v1/approvals/" + approval), staff, 200), "$.steps[0].id")
			.longValue();
		long directorUser = userIdOf(director);

		call(post("/api/v1/approvals/" + approval + "/steps/" + stepId + "/reassign"), manager, 403,
				Map.of("userId", directorUser, "note", "On leave"));
		String reassigned = call(post("/api/v1/approvals/" + approval + "/steps/" + stepId + "/reassign"), hr, 200,
				Map.of("userId", directorUser, "note", "Manager is on leave this week"));
		assertThat(JsonPath.<List<String>>read(reassigned, "$.history[*].action")).contains("REASSIGNED");

		assertError(decide(approval, manager, "approve", null, 403), "approval.not-eligible");
		assertThat(JsonPath.<String>read(decide(approval, director, "approve", null, 200), "$.status"))
			.isEqualTo("APPROVED");
	}

	@Test
	void whoMayFileAndSeeRequestsIsChecked() throws Exception {
		Map<String, Object> termination = Map.of("lastWorkingDay", today.plusMonths(1).toString(), "grounds",
				"RESTRUCTURING");
		// not self-service
		assertError(perform(post("/api/v1/me/requests"), staff, body("TERMINATION", termination), 403),
				"request.not-self-service");
		// a manager in the reporting line may file it; one outside it may not
		long id = id(perform(post("/api/v1/requests"), manager, onBehalf(staffId, "TERMINATION", termination), 201));
		assertError(perform(post("/api/v1/requests"), director, onBehalf(loneId, "TERMINATION", termination), 403),
				"request.cannot-file-for-employee");

		// outsiders cannot see it or its approval
		call(get("/api/v1/requests/" + id), colleague, 404);
		call(get("/api/v1/approvals/" + approvalOf(id)), colleague, 404);
		call(get("/api/v1/requests/" + id), staff, 200);
	}

	@Test
	void formsAndMonthlyLimitsAreEnforced() throws Exception {
		assertError(file(staff, "LOAN", Map.of("installments", 3), 422), "request.field-required");
		assertError(file(staff, "LOAN", Map.of("amount", 100, "installments", 3, "color", "red"), 422),
				"request.unknown-field");
		assertError(file(staff, "LOAN", Map.of("amount", 100, "installments", 99), 422),
				"request.field-out-of-range");
		assertError(file(staff, "EARLY_DEPARTURE", Map.of("date", today.toString(), "time", "25:00"), 422),
				"request.invalid-field");

		Map<String, Object> early = Map.of("date", today.toString(), "time", "15:00");
		file(staff, "EARLY_DEPARTURE", early);
		long second = id(file(staff, "EARLY_DEPARTURE", early));
		assertError(file(staff, "EARLY_DEPARTURE", early, 422), "request.monthly-limit");

		// withdrawing one frees the slot
		call(post("/api/v1/requests/" + second + "/cancel"), staff, 200, Map.of());
		file(staff, "EARLY_DEPARTURE", early);
	}

	@Test
	void companiesConfigureTheirOwnWorkflowsAndTypes() throws Exception {
		Map<String, Object> workflow = Map.of("code", "PAYROLL_ONLY", "nameEn", "Payroll", "nameAr", "الرواتب",
				"active", true, "steps", List.of(Map.of("stage", 1, "nameEn", "Payroll", "nameAr", "الرواتب",
						"approverType", "ROLE", "roleCode", "PAYROLL_ADMIN", "ifUnresolved", "FALLBACK")));
		call(post("/api/v1/workflows"), staff, 403, workflow);
		call(post("/api/v1/workflows"), hr, 201, workflow);

		Map<String, Object> type = new HashMap<>(Map.of("code", "BANK_LETTER", "nameEn", "Bank letter", "nameAr",
				"خطاب للبنك", "category", "OTHER", "workflowCode", "PAYROLL_ONLY", "selfService", true,
				"allowOnBehalf", false, "active", true, "sortOrder", 200));
		type.put("fields", List.of(Map.of("key", "bank", "labelEn", "Bank", "labelAr", "البنك", "type", "SELECT",
				"required", true, "options", List.of("NBE", "CIB", "QNB"))));
		call(post("/api/v1/request-types"), hr, 201, type);

		long approval = approvalOf(id(file(staff, "BANK_LETTER", Map.of("bank", "CIB"))));
		assertThat(inboxSubjects(manager)).isEmpty();
		assertThat(JsonPath.<String>read(decide(approval, payroll, "approve", null, 200), "$.status"))
			.isEqualTo("APPROVED");

		// a type cannot point at a workflow that does not exist
		type.put("workflowCode", "NOPE");
		assertError(call(put("/api/v1/request-types/BANK_LETTER"), hr, 422, type), "workflow.not-found");
	}

	@Test
	void leaveGoesThroughItsTypesWorkflow() throws Exception {
		// sick leave → MANAGER_HR: the manager's approval alone does not approve it
		LocalDate monday = today.plusDays(14).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
		long leave = id(call(post("/api/v1/me/leave/requests"), staff, 201, Map.of("leaveType", "SICK", "startDate",
				monday.toString(), "endDate", monday.toString())));
		String afterManager = call(post("/api/v1/leave/requests/" + leave + "/approve"), manager, 200, Map.of());
		assertThat(JsonPath.<String>read(afterManager, "$.status")).isEqualTo("PENDING");
		assertThat(JsonPath.<Integer>read(afterManager, "$.approval.currentStageNumber")).isEqualTo(2);

		String approved = call(post("/api/v1/leave/requests/" + leave + "/approve"), hr, 200, Map.of());
		assertThat(JsonPath.<String>read(approved, "$.status")).isEqualTo("APPROVED");
		assertThat(JsonPath.<String>read(approved, "$.approval.status")).isEqualTo("APPROVED");
	}

	@Test
	void editingALeaveTypeWithoutWorkflowCodeKeepsItsWorkflow() throws Exception {
		// the request body an older client sends: the type as listed, without workflowCode
		Map<String, Object> sick = new HashMap<>(JsonPath.<List<Map<String, Object>>>read(
				call(get("/api/v1/leave/types"), hr, 200), "$[?(@.code=='SICK')]").getFirst());
		assertThat(sick).containsEntry("workflowCode", "MANAGER_HR");
		sick.remove("id");
		sick.remove("workflowCode");
		sick.put("nameEn", "Sick leave (medical certificate)");

		String kept = call(put("/api/v1/leave/types/SICK"), hr, 200, sick);
		assertThat(JsonPath.<String>read(kept, "$.workflowCode")).isEqualTo("MANAGER_HR");

		sick.put("workflowCode", "");
		String reset = call(put("/api/v1/leave/types/SICK"), hr, 200, sick);
		assertThat(JsonPath.<String>read(reset, "$.workflowCode")).isNull();
	}

	@Test
	void theAuditTrailCannotBeRewritten() {
		// the application's database role may insert audit rows but never change or delete them
		assertThatThrownBy(() -> jdbc.update("update approval_actions set note = 'tampered'")).rootCause()
			.hasMessageContaining("permission denied");
		assertThatThrownBy(() -> jdbc.update("delete from approval_actions")).rootCause()
			.hasMessageContaining("permission denied");
	}

	// ------------------------------------------------------------------ helpers

	private String file(String token, String type, Map<String, Object> data) throws Exception {
		return file(token, type, data, 201);
	}

	private String file(String token, String type, Map<String, Object> data, int status) throws Exception {
		return perform(post("/api/v1/me/requests"), token, body(type, data), status);
	}

	private static String body(String type, Map<String, Object> data) {
		return JSON.writeValueAsString(Map.of("type", type, "data", data));
	}

	private static String onBehalf(long employeeId, String type, Map<String, Object> data) {
		return JSON.writeValueAsString(Map.of("employeeId", employeeId, "type", type, "data", data));
	}

	private long approvalOf(long requestId) throws Exception {
		return JsonPath.<Number>read(call(get("/api/v1/requests/" + requestId), owner, 200), "$.approval.id")
			.longValue();
	}

	private String decide(long approvalId, String token, String decision, String note, int status) throws Exception {
		Map<String, Object> body = note == null ? Map.of() : Map.of("note", note);
		return call(post("/api/v1/approvals/" + approvalId + "/" + decision), token, status, body);
	}

	private List<Long> inboxSubjects(String token) throws Exception {
		List<Number> ids = JsonPath.read(call(get("/api/v1/approvals/inbox"), token, 200), "$[*].subjectId");
		return ids.stream().map(Number::longValue).toList();
	}

	private long userIdOf(String token) throws Exception {
		return JsonPath.<Number>read(call(get("/api/v1/me"), token, 200), "$.id").longValue();
	}

	private long hire(String code, Long managerId) throws Exception {
		String body = """
				{"code": "%s", "fullNameEn": "Employee %1$s", "fullNameAr": "موظف %1$s", "gender": "MALE",
				 "birthDate": "1990-01-01", "hireDate": "%s", "managerId": %s}""".formatted(code,
				today.minusYears(3), managerId);
		return id(perform(post("/api/v1/employees"), owner, body, 201));
	}

	private String user(String email, String role, long employeeId) throws Exception {
		call(post("/api/v1/users"), owner, 201, Map.of("email", email, "fullName", email, "password",
				ApiClient.PASSWORD, "roles", List.of(role), "employeeId", employeeId));
		String json = perform(post("/api/v1/auth/login"), null, """
				{"tenant": "%s", "email": "%s", "password": "%s"}""".formatted(slug, email, ApiClient.PASSWORD), 200);
		return Tokens.of(json).bearer();
	}

	private String call(MockHttpServletRequestBuilder request, String token, int status) throws Exception {
		return perform(request, token, null, status);
	}

	private String call(MockHttpServletRequestBuilder request, String token, int status, Map<String, ?> body)
			throws Exception {
		return perform(request, token, JSON.writeValueAsString(body), status);
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

	private static long id(String json) {
		return JsonPath.<Number>read(json, "$.id").longValue();
	}

}
