package com.hrsaas.request.internal;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.hrsaas.identity.UserAccounts;

@Component
class RequestActorResolver {

	private final UserAccounts users;

	RequestActorResolver(UserAccounts users) {
		this.users = users;
	}

	RequestActor resolve(JwtAuthenticationToken authentication) {
		Long userId = Long.valueOf(authentication.getToken().getSubject());
		Set<String> authorities = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toUnmodifiableSet());
		return new RequestActor(userId, users.employeeIdOf(userId).orElse(null), authorities);
	}

}
