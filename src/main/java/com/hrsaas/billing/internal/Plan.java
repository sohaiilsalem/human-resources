package com.hrsaas.billing.internal;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import com.hrsaas.billing.Feature;
import com.hrsaas.shared.persistence.BaseEntity;

/** Platform table: plans and their features are data, so pricing changes need no deploy. */
@Entity
@Table(name = "plans")
class Plan extends BaseEntity {

	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@Column(nullable = false, length = 100)
	private String nameEn;

	@Column(nullable = false, length = 100)
	private String nameAr;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal monthlyPricePerEmployee;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal yearlyPricePerEmployee;

	private Integer maxEmployees;

	private Integer maxBranches;

	@Column(nullable = false)
	private boolean active;

	@Column(nullable = false)
	private int sortOrder;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "feature", nullable = false, length = 40)
	private Set<Feature> features = EnumSet.noneOf(Feature.class);

	protected Plan() {
	}

	boolean includes(Feature feature) {
		return features.contains(feature);
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

	BigDecimal getMonthlyPricePerEmployee() {
		return monthlyPricePerEmployee;
	}

	BigDecimal getYearlyPricePerEmployee() {
		return yearlyPricePerEmployee;
	}

	Integer getMaxEmployees() {
		return maxEmployees;
	}

	Integer getMaxBranches() {
		return maxBranches;
	}

	Set<Feature> getFeatures() {
		return Set.copyOf(features);
	}

}
