package com.hrsaas.workflow.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {

	List<ApprovalAction> findByInstanceIdOrderById(Long instanceId);

}
