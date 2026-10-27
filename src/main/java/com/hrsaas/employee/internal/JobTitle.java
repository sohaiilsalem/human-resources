package com.hrsaas.employee.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;

@Entity
@Table(name = "job_titles")
class JobTitle extends TenantEntity {

	@Column(nullable = false, updatable = false, length = 30)
	private String code;

	@Column(nullable = false, length = 100)
	private String nameEn;

	@Column(nullable = false, length = 100)
	private String nameAr;

	protected JobTitle() {
	}

	JobTitle(String code, String nameEn, String nameAr) {
		this.code = code.strip();
		apply(nameEn, nameAr);
	}

	void apply(String nameEn, String nameAr) {
		this.nameEn = nameEn.strip();
		this.nameAr = nameAr.strip();
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

}
