package com.hrsaas.identity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** User accounts of the current tenant. "Active" means the account is not disabled. */
public interface UserAccounts {

	/** The employee record a user account is linked to, if any. */
	Optional<Long> employeeIdOf(Long userId);

	/** The active account linked to an employee, if there is one. */
	Optional<Long> activeUserIdOfEmployee(Long employeeId);

	boolean isActive(Long userId);

	/** Role codes held by an active user, read from the database (not from a possibly stale token). */
	Set<String> roleCodesOf(Long userId);

	/** Permissions of an active user (union of their roles), read from the database. */
	Set<Permission> permissionsOf(Long userId);

	/** Active users holding the role, ordered by id. */
	List<Long> activeUserIdsWithRole(String roleCode);

	Map<Long, String> fullNamesOf(Collection<Long> userIds);

	Optional<RoleInfo> findRole(String code);

	Map<String, RoleInfo> findRoles(Collection<String> codes);

}
