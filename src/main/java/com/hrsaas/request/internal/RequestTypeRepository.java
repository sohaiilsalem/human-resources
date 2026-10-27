package com.hrsaas.request.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface RequestTypeRepository extends JpaRepository<RequestType, Long> {

	Optional<RequestType> findByCode(String code);

	boolean existsByCode(String code);

	List<RequestType> findAllByOrderBySortOrderAscIdAsc();

}
