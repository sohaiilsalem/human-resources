package com.hrsaas.organization.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {

	@Query("""
			select h from PublicHoliday h
			where h.startDate <= :to and h.endDate >= :from
			order by h.startDate""")
	List<PublicHoliday> findOverlapping(LocalDate from, LocalDate to);

}
