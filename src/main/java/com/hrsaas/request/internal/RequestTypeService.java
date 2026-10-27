package com.hrsaas.request.internal;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.shared.web.ApiException;
import com.hrsaas.workflow.Approvals;

/** Request type configuration ({@code request:manage}). Types are deactivated, never deleted. */
@Service
@Transactional
class RequestTypeService {

	private final RequestTypeRepository types;

	private final Approvals approvals;

	RequestTypeService(RequestTypeRepository types, Approvals approvals) {
		this.types = types;
		this.approvals = approvals;
	}

	@Transactional(readOnly = true)
	List<RequestType> list(boolean includeInactive) {
		return types.findAllByOrderBySortOrderAscIdAsc()
			.stream()
			.filter(type -> includeInactive || type.isActive())
			.toList();
	}

	@Transactional(readOnly = true)
	RequestType get(String code) {
		return types.findByCode(code).orElseThrow(() -> ApiException.notFound("request.type-not-found", code));
	}

	RequestType create(RequestTypeRequest request) {
		if (types.existsByCode(request.code())) {
			throw ApiException.conflict("request.type-code-taken", request.code());
		}
		check(request);
		return types.save(new RequestType(request));
	}

	/** Changes apply to requests filed afterwards; filed requests keep their values and their running approval. */
	RequestType update(String code, RequestTypeRequest request) {
		RequestType type = get(code);
		if (!type.getCode().equals(request.code())) {
			throw ApiException.unprocessable("request.type-code-immutable");
		}
		check(request);
		type.apply(request);
		return type;
	}

	private void check(RequestTypeRequest request) {
		RequestForm.checkDefinitions(request.fields());
		String workflow = request.workflowCode();
		if (workflow != null && !workflow.isBlank() && !approvals.workflowExists(workflow.strip())) {
			throw ApiException.unprocessable("workflow.not-found", workflow);
		}
	}

}
