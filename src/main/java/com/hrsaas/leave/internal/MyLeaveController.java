package com.hrsaas.leave.internal;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.billing.Feature;
import com.hrsaas.billing.RequiresFeature;
import com.hrsaas.leave.internal.LeaveViews.BalanceView;
import com.hrsaas.leave.internal.LeaveViews.RequestView;
import com.hrsaas.shared.web.ApiException;

/** Employee self-service: the caller's own balances and requests. */
@RestController
@RequestMapping("/api/v1/me/leave")
@RequiresFeature(Feature.LEAVE)
@PreAuthorize("hasAuthority('self:service')")
class MyLeaveController {

	private final LeaveRequestService requests;

	private final LeaveQueryService queries;

	private final ActorResolver actors;

	MyLeaveController(LeaveRequestService requests, LeaveQueryService queries, ActorResolver actors) {
		this.requests = requests;
		this.queries = queries;
		this.actors = actors;
	}

	@GetMapping("/balances")
	List<BalanceView> balances(JwtAuthenticationToken auth) {
		return queries.balances(employeeId(actors.resolve(auth)));
	}

	@GetMapping("/requests")
	PagedModel<RequestView> requests(JwtAuthenticationToken auth,
			@PageableDefault(sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
		return new PagedModel<>(queries.requestsOf(employeeId(actors.resolve(auth)), pageable));
	}

	@PostMapping("/requests")
	@ResponseStatus(HttpStatus.CREATED)
	RequestView submit(JwtAuthenticationToken auth, @Valid @RequestBody LeaveRequestInput input) {
		Actor actor = actors.resolve(auth);
		return queries.view(requests.submit(employeeId(actor), input, actor));
	}

	@PostMapping("/requests/{id}/cancel")
	RequestView cancel(JwtAuthenticationToken auth, @PathVariable UUID id, @Valid @RequestBody(required = false) Note note) {
		return queries.view(requests.cancel(id, actors.resolve(auth), note != null ? note.note() : null));
	}

	private static UUID employeeId(Actor actor) {
		if (actor.employeeId() == null) {
			throw ApiException.forbidden("leave.no-employee-profile");
		}
		return actor.employeeId();
	}

	record Note(@Size(max = 500) String note) {
	}

}
