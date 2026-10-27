package com.hrsaas.platform;

/** Published synchronously inside the sign-up transaction. */
public record TenantRegistered(Long tenantId, String slug, String name) {

}
