package com.hrsaas.employee.internal;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrsaas.shared.web.ApiException;

@Service
@Transactional(readOnly = true)
class JobTitleService {

	private final JobTitleRepository jobTitles;
	private final EmployeeRepository employees;

	JobTitleService(JobTitleRepository jobTitles, EmployeeRepository employees) {
		this.jobTitles = jobTitles;
		this.employees = employees;
	}

	List<JobTitle> list() {
		return jobTitles.findAllByOrderByNameEn();
	}

	JobTitle get(Long id) {
		return jobTitles.findById(id).orElseThrow(() -> ApiException.notFound("job-title.not-found", id));
	}

	@Transactional
	JobTitle create(CreateJobTitle request) {
		if (jobTitles.existsByCode(request.code().strip())) {
			throw ApiException.conflict("job-title.code-taken", request.code());
		}
		return jobTitles.save(new JobTitle(request.code(), request.nameEn(), request.nameAr()));
	}

	@Transactional
	JobTitle update(Long id, UpdateJobTitle request) {
		JobTitle jobTitle = get(id);
		jobTitle.apply(request.nameEn(), request.nameAr());
		return jobTitle;
	}

	@Transactional
	void delete(Long id) {
		JobTitle jobTitle = get(id);
		if (employees.existsByJobTitle_Id(id)) {
			throw ApiException.conflict("job-title.in-use", jobTitle.getCode());
		}
		jobTitles.delete(jobTitle);
	}

	record CreateJobTitle(@NotBlank @Pattern(regexp = "[A-Z0-9_]{2,30}", message = "{validation.code}") String code,
			@NotBlank @Size(max = 100) String nameEn, @NotBlank @Size(max = 100) String nameAr) {
	}

	record UpdateJobTitle(@NotBlank @Size(max = 100) String nameEn, @NotBlank @Size(max = 100) String nameAr) {
	}

}
