package com.hrsaas.shared.persistence;

import java.security.SecureRandom;
import java.util.UUID;

/** Time-ordered UUIDv7 identifiers (index-friendly, assigned before persist). */
public final class Ids {

	private static final SecureRandom RANDOM = new SecureRandom();

	private Ids() {
	}

	public static UUID newId() {
		long msb = (System.currentTimeMillis() << 16) | 0x7000L | (RANDOM.nextInt() & 0x0FFFL);
		long lsb = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
		return new UUID(msb, lsb);
	}

}
