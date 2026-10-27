package com.hrsaas.request.internal;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.request.RequestCategory;
import com.hrsaas.workflow.ApprovalView;

/** API response shapes. */
final class RequestViews {

	private RequestViews() {
	}

	record TypeView(Long id, String code, String nameEn, String nameAr, String descriptionEn, String descriptionAr,
			RequestCategory category, List<FieldDefinition> fields, String workflowCode, boolean selfService,
			boolean allowOnBehalf, Integer maxPerMonth, boolean active, int sortOrder) {

		static TypeView of(RequestType type) {
			return new TypeView(type.getId(), type.getCode(), type.getNameEn(), type.getNameAr(),
					type.getDescriptionEn(), type.getDescriptionAr(), type.getCategory(), type.getFields(),
					type.getWorkflowCode(), type.isSelfService(), type.isAllowOnBehalf(), type.getMaxPerMonth(),
					type.isActive(), type.getSortOrder());
		}

	}

	/** {@code approval} (the step-by-step timeline) is filled in for a single request, not in lists. */
	record RequestView(Long id, String type, String typeNameEn, String typeNameAr, RequestCategory category,
			Long employeeId, String employeeCode, String employeeNameEn, String employeeNameAr, RequestStatus status,
			Map<String, Object> data, String reason, Long requestedBy, Instant createdAt, Instant decidedAt,
			String decisionNote, ApprovalView approval) {

		static RequestView of(EmployeeRequest request, EmployeeInfo employee, ApprovalView approval) {
			RequestType type = request.getType();
			return new RequestView(request.getId(), type.getCode(), type.getNameEn(), type.getNameAr(),
					type.getCategory(), request.getEmployeeId(), employee != null ? employee.code() : null,
					employee != null ? employee.fullNameEn() : null, employee != null ? employee.fullNameAr() : null,
					request.getStatus(), request.getData(), request.getReason(), request.getRequestedBy(),
					request.getCreatedAt(), request.getDecidedAt(), request.getDecisionNote(), approval);
		}

	}

}
