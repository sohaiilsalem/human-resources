package com.hrsaas.shared.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import com.hrsaas.shared.tenant.TenantContext;

/**
 * Hands Hibernate pooled connections with {@code app.tenant_id} set, which the row-level security policies read.
 * The setting is reset before a connection goes back to the pool.
 */
class RlsConnectionProvider implements MultiTenantConnectionProvider<UUID> {

	private final DataSource dataSource;

	RlsConnectionProvider(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Connection getAnyConnection() throws SQLException {
		return getConnection(TenantContext.NO_TENANT);
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		releaseConnection(TenantContext.NO_TENANT, connection);
	}

	@Override
	public Connection getConnection(UUID tenantId) throws SQLException {
		Connection connection = dataSource.getConnection();
		try {
			bindTenant(connection, tenantId);
			return connection;
		}
		catch (SQLException ex) {
			connection.close();
			throw ex;
		}
	}

	@Override
	public void releaseConnection(UUID tenantId, Connection connection) throws SQLException {
		try (connection) {
			bindTenant(connection, TenantContext.NO_TENANT);
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> type) {
		return type.isInstance(this);
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		return type.cast(this);
	}

	private static void bindTenant(Connection connection, UUID tenantId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("select set_config('app.tenant_id', ?, false)")) {
			statement.setString(1, tenantId.toString());
			statement.execute();
		}
	}

}
