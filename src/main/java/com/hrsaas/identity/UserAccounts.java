package com.hrsaas.identity;

import java.util.Optional;

/** User accounts of the current tenant. */
public interface UserAccounts {

	/** The employee record a user account is linked to, if any. */
	Optional<Long> employeeIdOf(Long userId);

}
