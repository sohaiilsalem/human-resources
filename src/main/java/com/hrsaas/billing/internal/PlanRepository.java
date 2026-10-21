package com.hrsaas.billing.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PlanRepository extends JpaRepository<Plan, UUID> {

	Optional<Plan> findByCode(String code);

	List<Plan> findByActiveTrueOrderBySortOrder();

}
