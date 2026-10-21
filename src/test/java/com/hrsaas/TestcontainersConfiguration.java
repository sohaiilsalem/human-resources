package com.hrsaas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Postgres set up like production: Flyway migrates as the container's owner user, the application connects as the
 * non-owner {@code hr_app} role so row-level security is in force during tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).withCopyFileToContainer(
				MountableFile.forHostPath("docker/postgres/init/01-roles.sql"),
				"/docker-entrypoint-initdb.d/01-roles.sql");
	}

	@Bean
	DynamicPropertyRegistrar postgresProperties(PostgreSQLContainer postgres) {
		return registry -> {
			registry.add("spring.datasource.url", postgres::getJdbcUrl);
			registry.add("spring.datasource.username", () -> "hr_app");
			registry.add("spring.datasource.password", () -> "hr_app");
			registry.add("spring.flyway.user", postgres::getUsername);
			registry.add("spring.flyway.password", postgres::getPassword);
		};
	}

}
