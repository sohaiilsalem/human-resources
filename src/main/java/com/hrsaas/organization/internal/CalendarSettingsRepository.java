package com.hrsaas.organization.internal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CalendarSettingsRepository extends JpaRepository<CalendarSettings, UUID> {

	/** At most one row per tenant; the tenant filter is applied by Hibernate. */
	@Query("select c from CalendarSettings c")
	Optional<CalendarSettings> findCurrent();

}
