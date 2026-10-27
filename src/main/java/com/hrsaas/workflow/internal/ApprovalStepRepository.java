package com.hrsaas.workflow.internal;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

	/**
	 * Pending steps the user may decide: assigned to them, or to one of their roles. Never their own requests.
	 * @param roleCodes never empty (pass a placeholder when the user has no roles)
	 */
	@Query("""
			select s from ApprovalStep s join fetch s.instance i
			where s.status = com.hrsaas.workflow.ApprovalStepStatus.PENDING
			  and i.status = com.hrsaas.workflow.ApprovalStatus.IN_PROGRESS
			  and (s.assigneeUserId = :userId or (s.assigneeUserId is null and s.assigneeRoleCode in :roleCodes))
			  and i.employeeId <> :ownEmployeeId
			order by s.updatedAt""")
	List<ApprovalStep> inbox(Long userId, Collection<String> roleCodes, Long ownEmployeeId);

}
