package com.hrsaas.leave.internal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LeaveSettingsRepository extends JpaRepository<LeaveSettings, Long> {

	/** At most one row per tenant; the tenant filter is applied by Hibernate. */
	@Query("select s from LeaveSettings s")
	Optional<LeaveSettings> findCurrent();

}
