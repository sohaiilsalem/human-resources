package com.hrsaas.platform;

import java.util.UUID;

/** Published synchronously inside the sign-up transaction. */
public record TenantRegistered(UUID tenantId, String slug, String name) {

}
