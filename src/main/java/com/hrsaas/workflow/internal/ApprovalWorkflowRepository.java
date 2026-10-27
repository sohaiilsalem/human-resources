package com.hrsaas.workflow.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {

	Optional<ApprovalWorkflow> findByCode(String code);

	Optional<ApprovalWorkflow> findByDefaultWorkflowTrue();

	boolean existsByCode(String code);

	List<ApprovalWorkflow> findAllByOrderById();

}
