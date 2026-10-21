package com.hrsaas.shared.web;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Renders every API error as RFC 9457 problem details with a stable {@code code} and a localized {@code detail}. */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final MessageSource messages;

	GlobalExceptionHandler(MessageSource messages) {
		this.messages = messages;
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException ex, Locale locale) {
		return ResponseEntity.status(ex.status()).body(problem(ex.status(), ex.code(), ex.args(), locale));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex, Locale locale) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problem(HttpStatus.CONFLICT, "conflict", null, locale));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Locale locale = LocaleContextHolder.getLocale();
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation", null, locale);
		List<Map<String, String>> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> Map.of("field", error.getField(), "message", messages.getMessage(error, locale)))
			.toList();
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().headers(headers).body(problem);
	}

	private ProblemDetail problem(HttpStatus status, String code, Object[] args, Locale locale) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
				messages.getMessage("error." + code, args, code, locale));
		problem.setTitle(status.getReasonPhrase());
		problem.setProperty("code", code);
		return problem;
	}

}
