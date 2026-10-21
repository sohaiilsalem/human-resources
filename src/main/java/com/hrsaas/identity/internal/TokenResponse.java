package com.hrsaas.identity.internal;

record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
		long refreshExpiresIn) {

}
