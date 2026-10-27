package com.hrsaas.leave.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hrsaas.leave.LeaveCategory;

interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

	Optional<LeaveType> findByCode(String code);

	boolean existsByCode(String code);

	@Query("select t from LeaveType t order by t.category, t.code")
	List<LeaveType> findAllOrdered();

	Optional<LeaveType> findFirstByCategoryAndActiveTrueAndDeductsFromIsNull(LeaveCategory category);

}
