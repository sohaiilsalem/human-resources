package com.hrsaas.identity.internal;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("hrsaas.security.jwt")
record JwtProperties(String secret, @DefaultValue("hr-saas") String issuer,
		@DefaultValue("15m") Duration accessTokenTtl, @DefaultValue("30d") Duration refreshTokenTtl) {

	JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("hrsaas.security.jwt.secret must be at least 32 bytes (HS256)");
		}
	}

}
