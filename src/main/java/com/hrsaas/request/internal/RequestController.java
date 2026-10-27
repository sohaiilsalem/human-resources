package com.hrsaas.request.internal;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.billing.Feature;
import com.hrsaas.billing.RequiresFeature;
import com.hrsaas.request.internal.RequestViews.RequestView;
import com.hrsaas.request.internal.RequestViews.TypeView;
import com.hrsaas.shared.web.ApiException;

/** Request types, and requests seen from HR's and managers' side. Approving happens under /api/v1/approvals. */
@RestController
@RequestMapping("/api/v1")
@RequiresFeature(Feature.SELF_SERVICE)
class RequestController {

	private final RequestTypeService types;

	private final EmployeeRequestService requests;

	private final RequestQueryService queries;

	private final RequestActorResolver actors;

	RequestController(RequestTypeService types, EmployeeRequestService requests, RequestQueryService queries,
			RequestActorResolver actors) {
		this.types = types;
		this.requests = requests;
		this.queries = queries;
		this.actors = actors;
	}

	// ------------------------------------------------------------------ types

	/** Active types with their forms, for everyone; HR can include inactive ones. */
	@GetMapping("/request-types")
	List<TypeView> types(JwtAuthenticationToken auth, @RequestParam(defaultValue = "false") boolean includeInactive) {
		if (includeInactive && !actors.resolve(auth).canManage()) {
			throw ApiException.forbidden("request.cannot-manage");
		}
		return types.list(includeInactive).stream().map(TypeView::of).toList();
	}

	@GetMapping("/request-types/{code}")
	TypeView type(@PathVariable String code) {
		return TypeView.of(types.get(code));
	}

	@PostMapping("/request-types")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('request:manage')")
	TypeView createType(@Valid @RequestBody RequestTypeRequest request) {
		return TypeView.of(types.create(request));
	}

	@PutMapping("/request-types/{code}")
	@PreAuthorize("hasAuthority('request:manage')")
	TypeView updateType(@PathVariable String code, @Valid @RequestBody RequestTypeRequest request) {
		return TypeView.of(types.update(code, request));
	}

	// ------------------------------------------------------------------ requests

	@GetMapping("/requests")
	@PreAuthorize("hasAuthority('request:manage')")
	PagedModel<RequestView> search(@RequestParam(required = false) RequestStatus status,
			@RequestParam(required = false) String type, @RequestParam(required = false) Long employeeId,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return new PagedModel<>(queries.search(status, type, employeeId, pageable));
	}

	@GetMapping("/requests/{id}")
	RequestView get(JwtAuthenticationToken auth, @PathVariable Long id) {
		return queries.detail(requests.getVisible(id, actors.resolve(auth)));
	}

	/** Filed for someone else: by HR, or by a manager in the employee's reporting line where the type allows it. */
	@PostMapping("/requests")
	@ResponseStatus(HttpStatus.CREATED)
	RequestView submitFor(JwtAuthenticationToken auth, @Valid @RequestBody OnBehalf request) {
		RequestInput input = new RequestInput(request.type(), request.data(), request.reason());
		return queries.detail(requests.submit(request.employeeId(), input, actors.resolve(auth)));
	}

	@PostMapping("/requests/{id}/cancel")
	RequestView cancel(JwtAuthenticationToken auth, @PathVariable Long id,
			@Valid @RequestBody(required = false) Note note) {
		return queries.detail(requests.cancel(id, actors.resolve(auth), note != null ? note.note() : null));
	}

	record OnBehalf(@NotNull Long employeeId, @NotBlank String type, Map<String, Object> data,
			@Size(max = 1000) String reason) {
	}

	record Note(@Size(max = 500) String note) {
	}

}
