package com.hrsaas.request.internal;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.hrsaas.request.RequestCategory;
import com.hrsaas.shared.persistence.TenantEntity;

/**
 * A kind of request employees can file, configured as data: its form, who may file it, how often, and which approval
 * workflow decides it.
 */
@Entity
@Table(name = "request_types")
class RequestType extends TenantEntity {

	@Column(nullable = false, updatable = false, length = 30)
	private String code;

	@Column(nullable = false, length = 100)
	private String nameEn;

	@Column(nullable = false, length = 100)
	private String nameAr;

	@Column(length = 500)
	private String descriptionEn;

	@Column(length = 500)
	private String descriptionAr;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RequestCategory category;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private List<FieldDefinition> fields = new ArrayList<>();

	/** Null: the tenant's default workflow. */
	@Column(length = 30)
	private String workflowCode;

	/** Employees may file it for themselves. */
	@Column(nullable = false)
	private boolean selfService;

	/** Managers may file it for people in their reporting line (HR always may). */
	@Column(nullable = false)
	private boolean allowOnBehalf;

	private Integer maxPerMonth;

	@Column(nullable = false)
	private boolean active;

	@Column(nullable = false)
	private int sortOrder;

	protected RequestType() {
	}

	RequestType(RequestTypeRequest request) {
		this.code = request.code();
		apply(request);
	}

	void apply(RequestTypeRequest request) {
		this.nameEn = request.nameEn().strip();
		this.nameAr = request.nameAr().strip();
		this.descriptionEn = blankToNull(request.descriptionEn());
		this.descriptionAr = blankToNull(request.descriptionAr());
		this.category = request.category();
		this.fields = new ArrayList<>(request.fields());
		this.workflowCode = blankToNull(request.workflowCode());
		this.selfService = request.selfService();
		this.allowOnBehalf = request.allowOnBehalf();
		this.maxPerMonth = request.maxPerMonth();
		this.active = request.active();
		this.sortOrder = request.sortOrder();
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	String getCode() {
		return code;
	}

	String getNameEn() {
		return nameEn;
	}

	String getNameAr() {
		return nameAr;
	}

	String getDescriptionEn() {
		return descriptionEn;
	}

	String getDescriptionAr() {
		return descriptionAr;
	}

	RequestCategory getCategory() {
		return category;
	}

	List<FieldDefinition> getFields() {
		return List.copyOf(fields);
	}

	String getWorkflowCode() {
		return workflowCode;
	}

	boolean isSelfService() {
		return selfService;
	}

	boolean isAllowOnBehalf() {
		return allowOnBehalf;
	}

	Integer getMaxPerMonth() {
		return maxPerMonth;
	}

	boolean isActive() {
		return active;
	}

	int getSortOrder() {
		return sortOrder;
	}

}
