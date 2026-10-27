package com.hrsaas.request.internal;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.billing.Feature;
import com.hrsaas.billing.RequiresFeature;
import com.hrsaas.request.internal.RequestViews.RequestView;
import com.hrsaas.shared.web.ApiException;

/** Employee self-service: the caller's own requests. Details and withdrawal use /api/v1/requests/{id}. */
@RestController
@RequestMapping("/api/v1/me/requests")
@RequiresFeature(Feature.SELF_SERVICE)
@PreAuthorize("hasAuthority('self:service')")
class MyRequestController {

	private final EmployeeRequestService requests;

	private final RequestQueryService queries;

	private final RequestActorResolver actors;

	MyRequestController(EmployeeRequestService requests, RequestQueryService queries, RequestActorResolver actors) {
		this.requests = requests;
		this.queries = queries;
		this.actors = actors;
	}

	@GetMapping
	PagedModel<RequestView> list(JwtAuthenticationToken auth,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return new PagedModel<>(queries.requestsOf(employeeId(actors.resolve(auth)), pageable));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	RequestView submit(JwtAuthenticationToken auth, @Valid @RequestBody RequestInput input) {
		RequestActor actor = actors.resolve(auth);
		return queries.detail(requests.submit(employeeId(actor), input, actor));
	}

	private static Long employeeId(RequestActor actor) {
		if (actor.employeeId() == null) {
			throw ApiException.forbidden("request.no-employee-profile");
		}
		return actor.employeeId();
	}

}
