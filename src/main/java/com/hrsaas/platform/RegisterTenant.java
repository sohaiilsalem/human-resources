package com.hrsaas.platform;

import java.util.UUID;

/** {@code id} is chosen by the caller so tenant-scoped rows can be written in the same transaction. */
public record RegisterTenant(UUID id, String slug, String name, String defaultLocale) {

}
