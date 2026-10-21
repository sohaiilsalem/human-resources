package com.hrsaas.organization.internal;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.hrsaas.shared.persistence.TenantEntity;

/** An official holiday, possibly spanning several days (e.g. Eid al-Fitr). */
@Entity
@Table(name = "public_holidays")
class PublicHoliday extends TenantEntity {

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	@Column(nullable = false, length = 200)
	private String nameEn;

	@Column(nullable = false, length = 200)
	private String nameAr;

	protected PublicHoliday() {
	}

	PublicHoliday(LocalDate startDate, LocalDate endDate, String nameEn, String nameAr) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.nameEn = nameEn;
		this.nameAr = nameAr;
	}

	LocalDate getStartDate() {
		return startDate;
	}

	LocalDate getEndDate() {
		return endDate;
	}

	String getNameEn() {
		return nameEn;
	}

	String getNameAr() {
		return nameAr;
	}

}
