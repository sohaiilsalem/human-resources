/**
 * Shared kernel: tenant context, persistence base types, error handling, i18n and cross-cutting configuration.
 * Open module — every other module may use any of its packages.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.hrsaas.shared;

import org.springframework.modulith.ApplicationModule;
