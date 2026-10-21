package com.hrsaas.identity.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

final class AuthRequests {

	static final String SLUG = "^[a-z0-9](?:[a-z0-9-]{1,38}[a-z0-9])$";

	private AuthRequests() {
	}

	record SignUp(@NotBlank @Size(max = 200) String companyName,
			@NotBlank @Pattern(regexp = SLUG, message = "{validation.tenant-slug}") String tenant,
			@NotBlank @Size(max = 200) String fullName, @NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(min = 8, max = 128) String password,
			@Pattern(regexp = "en|ar", message = "{validation.locale}") String locale) {
	}

	record Login(@NotBlank String tenant, @NotBlank String email, @NotBlank String password) {
	}

	record Refresh(@NotBlank String refreshToken) {
	}

}
