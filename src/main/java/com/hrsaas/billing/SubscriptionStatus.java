package com.hrsaas.billing;

/** {@code TRIAL → ACTIVE → PAST_DUE → (grace) → SUSPENDED → CANCELLED} (ARCHITECTURE §6). */
public enum SubscriptionStatus {

	TRIAL, ACTIVE, PAST_DUE, SUSPENDED, CANCELLED

}
