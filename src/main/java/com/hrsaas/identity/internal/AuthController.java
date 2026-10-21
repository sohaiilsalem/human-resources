package com.hrsaas.identity.internal;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	private final AuthService auth;

	AuthController(AuthService auth) {
		this.auth = auth;
	}

	/** Registers a company (tenant) with its owner account and logs the owner in. */
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	TokenResponse signUp(@Valid @RequestBody AuthRequests.SignUp request) {
		return auth.signUp(request);
	}

	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody AuthRequests.Login request) {
		return auth.login(request);
	}

	@PostMapping("/refresh")
	TokenResponse refresh(@Valid @RequestBody AuthRequests.Refresh request) {
		return auth.refresh(request.refreshToken());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(@Valid @RequestBody AuthRequests.Refresh request) {
		auth.logout(request.refreshToken());
	}

}
