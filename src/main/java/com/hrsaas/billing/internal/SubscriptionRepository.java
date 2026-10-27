package com.hrsaas.billing.internal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findByTenantId(Long tenantId);

}
