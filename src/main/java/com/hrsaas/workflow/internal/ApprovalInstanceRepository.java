package com.hrsaas.workflow.internal;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.hrsaas.workflow.ApprovalStatus;

interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {

	Optional<ApprovalInstance> findBySubjectTypeAndSubjectId(String subjectType, Long subjectId);

	boolean existsBySubjectTypeAndSubjectId(String subjectType, Long subjectId);

	/** Serializes decisions on one approval, e.g. two parallel approvers clicking at the same time. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from ApprovalInstance i where i.id = :id")
	Optional<ApprovalInstance> lockById(Long id);

	@Query("select i from ApprovalInstance i where (:status is null or i.status = :status)")
	Page<ApprovalInstance> search(ApprovalStatus status, Pageable pageable);

}
