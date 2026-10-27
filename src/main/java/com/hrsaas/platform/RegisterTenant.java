package com.hrsaas.platform;

/**
 * {@code id} comes from {@link TenantDirectory#reserveId()} so tenant-scoped rows can be written in the same
 * transaction.
 */
public record RegisterTenant(Long id, String slug, String name, String defaultLocale) {

}
