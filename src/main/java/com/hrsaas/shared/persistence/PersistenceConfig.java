package com.hrsaas.shared.persistence;

import java.util.Optional;

import javax.sql.DataSource;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditor")
class PersistenceConfig {

	@Bean
	HibernatePropertiesCustomizer multiTenancy(DataSource dataSource) {
		return properties -> {
			properties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, new RlsConnectionProvider(dataSource));
			properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new TenantIdentifierResolver());
		};
	}

	@Bean
	AuditorAware<Long> auditor() {
		return () -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
				return Optional.of(Long.valueOf(jwt.getSubject()));
			}
			return Optional.empty();
		};
	}

}
