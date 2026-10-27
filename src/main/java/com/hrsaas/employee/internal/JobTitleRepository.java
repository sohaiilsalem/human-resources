package com.hrsaas.employee.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface JobTitleRepository extends JpaRepository<JobTitle, Long> {

	boolean existsByCode(String code);

	List<JobTitle> findAllByOrderByNameEn();

}
