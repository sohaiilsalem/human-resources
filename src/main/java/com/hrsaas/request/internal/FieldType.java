package com.hrsaas.request.internal;

/** Input types of request form fields, and how a submitted value is stored. */
enum FieldType {

	/** Up to 200 characters. */
	TEXT,
	/** Up to 2,000 characters. */
	LONG_TEXT,
	NUMBER,
	/** A number with at most two decimals, e.g. EGP. */
	AMOUNT,
	/** {@code yyyy-MM-dd} */
	DATE,
	/** {@code HH:mm} */
	TIME,
	BOOLEAN,
	/** One of {@code options}. */
	SELECT

}
