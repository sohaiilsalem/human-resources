package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
import com.hrsaas.leave.internal.LeaveViews.BalanceView;
import com.hrsaas.leave.internal.LeaveViews.RequestView;
import com.hrsaas.leave.internal.LeaveViews.SettingsView;
import com.hrsaas.leave.internal.LeaveViews.TransactionView;
import com.hrsaas.leave.internal.LeaveViews.TypeView;
import com.hrsaas.shared.web.ApiException;

/** HR and manager side of leave management. */
@RestController
@RequestMapping("/api/v1/leave")
@RequiresFeature(Feature.LEAVE)
class LeaveController {

	private final LeaveRequestService requests;

	private final LeaveQueryService queries;

	private final LeaveAdminService admin;

	private final ActorResolver actors;

	LeaveController(LeaveRequestService requests, LeaveQueryService queries, LeaveAdminService admin,
			ActorResolver actors) {
		this.requests = requests;
		this.queries = queries;
		this.admin = admin;
		this.actors = actors;
	}

	// ------------------------------------------------------------------ types & settings

	@GetMapping("/types")
	List<TypeView> types() {
		return admin.types().stream().map(TypeView::of).toList();
	}

	@PostMapping("/types")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('leave:manage')")
	TypeView createType(@Valid @RequestBody LeaveTypeRequest request) {
		return TypeView.of(admin.createType(request));
	}

	@PutMapping("/types/{code}")
	@PreAuthorize("hasAuthority('leave:manage')")
	TypeView updateType(@PathVariable String code, @Valid @RequestBody LeaveTypeRequest request) {
		return TypeView.of(admin.updateType(code, request));
	}

	@GetMapping("/settings")
	@PreAuthorize("hasAuthority('leave:manage')")
	SettingsView settings() {
		return admin.settings();
	}

	@PutMapping("/settings")
	@PreAuthorize("hasAuthority('leave:manage')")
	SettingsView updateSettings(@Valid @RequestBody SettingsView request) {
		return admin.updateSettings(request);
	}

	// ------------------------------------------------------------------ requests

	@GetMapping("/requests")
	@PreAuthorize("hasAnyAuthority('leave:manage', 'leave:approve')")
	PagedModel<RequestView> search(JwtAuthenticationToken auth,
			@RequestParam(required = false) LeaveRequestStatus status,
			@RequestParam(required = false) Long employeeId,
			@PageableDefault(sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
		return new PagedModel<>(queries.search(actors.resolve(auth), status, employeeId, pageable));
	}

	@GetMapping("/requests/{id}")
	RequestView get(JwtAuthenticationToken auth, @PathVariable Long id) {
		return queries.view(requests.getVisible(id, actors.resolve(auth)));
	}

	/** HR records leave on an employee's behalf, e.g. sick leave reported by phone. */
	@PostMapping("/requests")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('leave:manage')")
	RequestView submitFor(JwtAuthenticationToken auth, @Valid @RequestBody OnBehalf request) {
		LeaveRequestInput input = new LeaveRequestInput(request.leaveType(), request.startDate(), request.endDate(),
				request.halfDay(), request.reason());
		return queries.view(requests.submit(request.employeeId(), input, actors.resolve(auth)));
	}

	@PostMapping("/requests/{id}/approve")
	@PreAuthorize("hasAnyAuthority('leave:manage', 'leave:approve')")
	RequestView approve(JwtAuthenticationToken auth, @PathVariable Long id,
			@Valid @RequestBody(required = false) MyLeaveController.Note note) {
		return queries.view(requests.approve(id, actors.resolve(auth), note != null ? note.note() : null));
	}

	@PostMapping("/requests/{id}/reject")
	@PreAuthorize("hasAnyAuthority('leave:manage', 'leave:approve')")
	RequestView reject(JwtAuthenticationToken auth, @PathVariable Long id,
			@Valid @RequestBody(required = false) MyLeaveController.Note note) {
		return queries.view(requests.reject(id, actors.resolve(auth), note != null ? note.note() : null));
	}

	@PostMapping("/requests/{id}/cancel")
	@PreAuthorize("hasAuthority('leave:manage')")
	RequestView cancel(JwtAuthenticationToken auth, @PathVariable Long id,
			@Valid @RequestBody(required = false) MyLeaveController.Note note) {
		return queries.view(requests.cancel(id, actors.resolve(auth), note != null ? note.note() : null));
	}

	// ------------------------------------------------------------------ balances & ledger

	@GetMapping("/employees/{employeeId}/balances")
	@PreAuthorize("hasAnyAuthority('leave:manage', 'leave:approve')")
	List<BalanceView> balances(JwtAuthenticationToken auth, @PathVariable Long employeeId) {
		if (!queries.canSeeEmployee(actors.resolve(auth), employeeId)) {
			throw ApiException.forbidden("leave.not-your-team");
		}
		return queries.balances(employeeId);
	}

	@GetMapping("/employees/{employeeId}/transactions")
	@PreAuthorize("hasAuthority('leave:manage')")
	List<TransactionView> transactions(@PathVariable Long employeeId) {
		return queries.history(employeeId);
	}

	@PostMapping("/adjustments")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('leave:manage')")
	TransactionView adjust(@Valid @RequestBody Adjustment request) {
		LeaveTransaction t = admin.adjust(request.employeeId(), request.leaveType(), request.days(), request.note());
		return new TransactionView(t.getId(), request.leaveType(), t.getLeaveYear(), t.getKind(), t.getDays(),
				t.getEffectiveDate(), null, t.getNote(), t.getCreatedAt());
	}

	record OnBehalf(@NotNull Long employeeId, @NotBlank String leaveType, @NotNull LocalDate startDate,
			@NotNull LocalDate endDate, boolean halfDay, @Size(max = 500) String reason) {
	}

	record Adjustment(@NotNull Long employeeId, @NotBlank String leaveType,
			@NotNull @DecimalMin("-366") @DecimalMax("366") @Digits(integer = 3, fraction = 1) BigDecimal days,
			@NotBlank @Size(max = 500) String note) {
	}

}
