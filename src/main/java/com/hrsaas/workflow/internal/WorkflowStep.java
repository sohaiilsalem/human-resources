package com.hrsaas.workflow.internal;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * One step of a workflow definition. Steps with the same {@code stage} run in parallel and all must approve; stages
 * run in ascending order. An optional condition ({@code amount GT 20000}) limits the step to matching requests.
 */
@Embeddable
class WorkflowStep {

	/** Fallback when a step names none: the owner role always exists. */
	static final String DEFAULT_FALLBACK_ROLE = "OWNER";

	@Column(nullable = false)
	private int stage;

	@Column(nullable = false, length = 100)
	private String nameEn;

	@Column(nullable = false, length = 100)
	private String nameAr;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApproverType approverType;

	private Integer managerLevel;

	@Column(length = 30)
	private String roleCode;

	private Long userId;

	@Column(length = 40)
	private String conditionField;

	@Enumerated(EnumType.STRING)
	@Column(length = 5)
	private ConditionOperator conditionOperator;

	@Column(precision = 14, scale = 2)
	private BigDecimal conditionValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "if_unresolved", nullable = false, length = 20)
	private UnresolvedPolicy ifUnresolved;

	@Column(length = 30)
	private String fallbackRoleCode;

	protected WorkflowStep() {
	}

	WorkflowStep(int stage, String nameEn, String nameAr, ApproverType approverType, Integer managerLevel,
			String roleCode, Long userId, String conditionField, ConditionOperator conditionOperator,
			BigDecimal conditionValue, UnresolvedPolicy ifUnresolved, String fallbackRoleCode) {
		this.stage = stage;
		this.nameEn = nameEn.strip();
		this.nameAr = nameAr.strip();
		this.approverType = approverType;
		this.managerLevel = approverType == ApproverType.MANAGER_LEVEL ? managerLevel : null;
		this.roleCode = approverType == ApproverType.ROLE ? roleCode : null;
		this.userId = approverType == ApproverType.USER ? userId : null;
		this.conditionField = conditionField;
		this.conditionOperator = conditionOperator;
		this.conditionValue = conditionValue;
		this.ifUnresolved = ifUnresolved;
		this.fallbackRoleCode = fallbackRoleCode;
	}

	static WorkflowStep directManager(int stage, UnresolvedPolicy ifUnresolved, String fallbackRole) {
		return new WorkflowStep(stage, "Direct manager", "المدير المباشر", ApproverType.DIRECT_MANAGER, null, null,
				null, null, null, null, ifUnresolved, fallbackRole);
	}

	static WorkflowStep role(int stage, String nameEn, String nameAr, String roleCode, UnresolvedPolicy ifUnresolved,
			String fallbackRole) {
		return new WorkflowStep(stage, nameEn, nameAr, ApproverType.ROLE, null, roleCode, null, null, null, null,
				ifUnresolved, fallbackRole);
	}

	WorkflowStep when(String field, ConditionOperator operator, BigDecimal value) {
		this.conditionField = field;
		this.conditionOperator = operator;
		this.conditionValue = value;
		return this;
	}

	/** A step without a condition always applies; one whose attribute is missing never does. */
	boolean appliesTo(Map<String, BigDecimal> attributes) {
		if (conditionField == null) {
			return true;
		}
		BigDecimal actual = attributes.get(conditionField);
		return actual != null && conditionOperator.test(actual, conditionValue);
	}

	String fallbackRole() {
		return fallbackRoleCode != null ? fallbackRoleCode : DEFAULT_FALLBACK_ROLE;
	}

	int getStage() {
		return stage;
	}

	String getNameEn() {
		return nameEn;
	}

	String getNameAr() {
		return nameAr;
	}

	ApproverType getApproverType() {
		return approverType;
	}

	Integer getManagerLevel() {
		return managerLevel;
	}

	String getRoleCode() {
		return roleCode;
	}

	Long getUserId() {
		return userId;
	}

	String getConditionField() {
		return conditionField;
	}

	ConditionOperator getConditionOperator() {
		return conditionOperator;
	}

	BigDecimal getConditionValue() {
		return conditionValue;
	}

	UnresolvedPolicy getIfUnresolved() {
		return ifUnresolved;
	}

	String getFallbackRoleCode() {
		return fallbackRoleCode;
	}

}
