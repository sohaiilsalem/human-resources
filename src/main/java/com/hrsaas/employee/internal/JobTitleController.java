package com.hrsaas.employee.internal;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrsaas.billing.Feature;
import com.hrsaas.billing.RequiresFeature;
import com.hrsaas.employee.JobTitleInfo;

@RestController
@RequestMapping("/api/v1/job-titles")
@RequiresFeature(Feature.CORE_HR)
class JobTitleController {

	private final JobTitleService jobTitles;

	JobTitleController(JobTitleService jobTitles) {
		this.jobTitles = jobTitles;
	}

	@GetMapping
	List<JobTitleInfo> list() {
		return jobTitles.list().stream().map(JobTitleController::info).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('organization:manage')")
	JobTitleInfo create(@Valid @RequestBody JobTitleService.CreateJobTitle request) {
		return info(jobTitles.create(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('organization:manage')")
	JobTitleInfo update(@PathVariable Long id, @Valid @RequestBody JobTitleService.UpdateJobTitle request) {
		return info(jobTitles.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('organization:manage')")
	void delete(@PathVariable Long id) {
		jobTitles.delete(id);
	}

	private static JobTitleInfo info(JobTitle title) {
		return new JobTitleInfo(title.getId(), title.getCode(), title.getNameEn(), title.getNameAr());
	}

}
