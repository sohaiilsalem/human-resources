package com.hrsaas.billing.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface PlanRepository extends JpaRepository<Plan, Long> {

	Optional<Plan> findByCode(String code);

	List<Plan> findByActiveTrueOrderBySortOrder();

}
