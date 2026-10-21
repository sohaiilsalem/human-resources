package com.hrsaas.shared.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * The tenant the current unit of work runs for. Bound per request (from the JWT {@code tid} claim) or explicitly via
 * {@link #callAs} for login, sign-up, jobs and event listeners. Never taken from a client-supplied header.
 */
public final class TenantContext {

	/** Sentinel used when no tenant is bound: matches no row under row-level security. */
	public static final UUID NO_TENANT = new UUID(0L, 0L);

	private static final ScopedValue<UUID> CURRENT = ScopedValue.newInstance();

	private TenantContext() {
	}

	public static Optional<UUID> current() {
		return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
	}

	public static UUID require() {
		return current().orElseThrow(() -> new IllegalStateException("No tenant bound to the current context"));
	}

	public static <T, X extends Throwable> T callAs(UUID tenantId, ScopedValue.CallableOp<T, X> action) throws X {
		return ScopedValue.where(CURRENT, tenantId).call(action);
	}

	public static void runAs(UUID tenantId, Runnable action) {
		ScopedValue.where(CURRENT, tenantId).run(action);
	}

}
