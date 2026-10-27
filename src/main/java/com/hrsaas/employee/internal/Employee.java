package com.hrsaas.employee.internal;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.employee.JobTitleInfo;
import com.hrsaas.employee.EmployeeStatus;
import com.hrsaas.employee.Gender;
import com.hrsaas.shared.persistence.TenantEntity;

/** Employee master record. Minimal for now (fields leave needs); Phase 1 adds org placement, contracts, documents. */
@Entity
@Table(name = "employees")
class Employee extends TenantEntity {

	@Column(nullable = false, length = 30)
	private String code;

	@Column(nullable = false, length = 200)
	private String fullNameEn;

	@Column(nullable = false, length = 200)
	private String fullNameAr;

	@Column(length = 14)
	private String nationalId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Gender gender;

	@Column(nullable = false)
	private LocalDate birthDate;

	@Column(nullable = false)
	private LocalDate hireDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EmployeeStatus status;

	private LocalDate terminationDate;

	private Long managerId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_title_id")
	private JobTitle jobTitle;

	@Column(nullable = false)
	private boolean disability;

	protected Employee() {
	}

	Employee(EmployeeRequest request, JobTitle jobTitle) {
		this.status = EmployeeStatus.ACTIVE;
		apply(request, jobTitle);
	}

	void apply(EmployeeRequest request, JobTitle jobTitle) {
		this.code = request.code().strip();
		this.fullNameEn = request.fullNameEn().strip();
		this.fullNameAr = request.fullNameAr().strip();
		this.nationalId = request.nationalId();
		this.gender = request.gender();
		this.birthDate = request.birthDate();
		this.hireDate = request.hireDate();
		this.managerId = request.managerId();
		this.jobTitle = jobTitle;
		this.disability = request.disability();
	}

	void terminate(LocalDate date) {
		this.status = EmployeeStatus.TERMINATED;
		this.terminationDate = date;
	}

	boolean isActive() {
		return status == EmployeeStatus.ACTIVE;
	}

	String getCode() {
		return code;
	}

	String getNationalId() {
		return nationalId;
	}

	JobTitle getJobTitle() {
		return jobTitle;
	}

	EmployeeInfo toInfo() {
		return new EmployeeInfo(getId(), code, fullNameEn, fullNameAr, gender, birthDate, hireDate, status,
				terminationDate, managerId, disability,
				jobTitle == null ? null : new JobTitleInfo(jobTitle.getId(), jobTitle.getCode(), jobTitle.getNameEn(),
						jobTitle.getNameAr()));
	}

}
