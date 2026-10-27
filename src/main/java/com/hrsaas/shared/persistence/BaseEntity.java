package com.hrsaas.shared.persistence;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/** Numeric id drawn from the table's identity column on insert: 1, 2, 3… per table. */
@MappedSuperclass
public abstract class BaseEntity extends AuditedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Override
	public Long getId() {
		return id;
	}

}
