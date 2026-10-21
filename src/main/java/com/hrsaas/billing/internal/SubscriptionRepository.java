package com.hrsaas.billing.internal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

	Optional<Subscription> findByTenantId(UUID tenantId);

}
