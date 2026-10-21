package com.hrsaas.shared.web;

import org.springframework.http.HttpStatus;

/**
 * Business error rendered as {@code application/problem+json}. {@code code} is stable for clients and doubles as the
 * message key {@code error.<code>} for the localized {@code detail}.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private final Object[] args;

	public ApiException(HttpStatus status, String code, Object... args) {
		super(code);
		this.status = status;
		this.code = code;
		this.args = args;
	}

	public static ApiException notFound(String code, Object... args) {
		return new ApiException(HttpStatus.NOT_FOUND, code, args);
	}

	public static ApiException conflict(String code, Object... args) {
		return new ApiException(HttpStatus.CONFLICT, code, args);
	}

	public static ApiException unauthorized(String code, Object... args) {
		return new ApiException(HttpStatus.UNAUTHORIZED, code, args);
	}

	public static ApiException forbidden(String code, Object... args) {
		return new ApiException(HttpStatus.FORBIDDEN, code, args);
	}

	/** A business rule rejects an otherwise well-formed request. */
	public static ApiException unprocessable(String code, Object... args) {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, code, args);
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public Object[] args() {
		return args;
	}

}
