package com.hrsaas.identity.internal;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hrsaas.shared.tenant.TenantContext;

/** Binds the tenant from the authenticated JWT ({@code tid}) for the rest of the request. */
class TenantContextFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken token)) {
			chain.doFilter(request, response);
			return;
		}
		UUID tenantId = UUID.fromString(token.getToken().getClaimAsString(TokenService.TENANT_CLAIM));
		MDC.put("tenantId", tenantId.toString());
		try {
			TenantContext.callAs(tenantId, () -> {
				chain.doFilter(request, response);
				return null;
			});
		}
		catch (IOException | ServletException | RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new ServletException(ex);
		}
		finally {
			MDC.remove("tenantId");
		}
	}

}
