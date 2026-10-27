package com.hrsaas.leave.internal;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.hrsaas.identity.UserAccounts;

@Component
class ActorResolver {

	private final UserAccounts users;

	ActorResolver(UserAccounts users) {
		this.users = users;
	}

	Actor resolve(JwtAuthenticationToken authentication) {
		Long userId = Long.valueOf(authentication.getToken().getSubject());
		Set<String> authorities = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toUnmodifiableSet());
		return new Actor(userId, users.employeeIdOf(userId).orElse(null), authorities);
	}

}
