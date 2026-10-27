package com.hrsaas.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.hrsaas.ApiClient;
import com.hrsaas.IntegrationTest;
import com.hrsaas.platform.TenantDirectory;
import com.hrsaas.shared.tenant.TenantContext;

@IntegrationTest
class TenantIsolationIntegrationTest {

	@Autowired
	ApiClient api;

	@Autowired
	TenantDirectory tenants;

	@Autowired
	UserRepository users;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void applicationRoleCannotBypassRowLevelSecurity() {
		Boolean privileged = jdbc.queryForObject(
				"select rolsuper or rolbypassrls from pg_roles where rolname = current_user", Boolean.class);
		assertThat(privileged).isFalse();
	}

	@Test
	void sameEmailInTwoCompaniesStaysSeparate() throws Exception {
		String slugA = ApiClient.uniqueSlug("iso-a");
		String slugB = ApiClient.uniqueSlug("iso-b");
		api.signUp(slugA, "shared@example.com");
		api.signUp(slugB, "shared@example.com");
		Long tenantA = tenants.findBySlug(slugA).orElseThrow().id();
		Long tenantB = tenants.findBySlug(slugB).orElseThrow().id();

		User userA = TenantContext.callAs(tenantA, () -> users.findByEmail("shared@example.com").orElseThrow());
		User userB = TenantContext.callAs(tenantB, () -> users.findByEmail("shared@example.com").orElseThrow());
		assertThat(userA.getId()).isNotEqualTo(userB.getId());
		assertThat(userA.getTenantId()).isEqualTo(tenantA);

		assertThat(TenantContext.callAs(tenantB, () -> users.findById(userA.getId()))).isEmpty();
		assertThat(TenantContext.callAs(tenantA, () -> users.findAll()))
			.allSatisfy(user -> assertThat(user.getTenantId()).isEqualTo(tenantA));
		assertThat(users.findAll()).as("no tenant bound").isEmpty();
	}

	@Test
	void databaseRejectsCrossTenantRowsEvenWithRawSql() throws Exception {
		String slugA = ApiClient.uniqueSlug("rls-a");
		String slugB = ApiClient.uniqueSlug("rls-b");
		api.signUp(slugA, "a@example.com");
		api.signUp(slugB, "b@example.com");
		Long tenantA = tenants.findBySlug(slugA).orElseThrow().id();

		assertThat(emailsVisibleTo(null)).isEmpty();
		assertThat(emailsVisibleTo(tenantA)).containsExactly("a@example.com");
		assertThat(rolesVisibleTo(tenantA)).containsExactly("OWNER");
	}

	private List<String> emailsVisibleTo(Long tenantId) {
		return queryAs(tenantId, "select email from users");
	}

	private List<String> rolesVisibleTo(Long tenantId) {
		return queryAs(tenantId, "select r.code from user_roles ur join roles r on r.id = ur.role_id");
	}

	private List<String> queryAs(Long tenantId, String sql) {
		return jdbc.execute((ConnectionCallback<List<String>>) connection -> {
			try (PreparedStatement set = connection.prepareStatement("select set_config('app.tenant_id', ?, false)")) {
				set.setString(1, tenantId == null ? "" : tenantId.toString());
				set.execute();
			}
			List<String> values = new ArrayList<>();
			try (PreparedStatement query = connection.prepareStatement(sql); ResultSet rows = query.executeQuery()) {
				while (rows.next()) {
					values.add(rows.getString(1));
				}
			}
			finally {
				try (PreparedStatement reset = connection.prepareStatement("select set_config('app.tenant_id', '', false)")) {
					reset.execute();
				}
			}
			return values;
		});
	}

}
