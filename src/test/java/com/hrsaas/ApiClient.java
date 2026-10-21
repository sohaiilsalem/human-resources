package com.hrsaas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Test helper for the auth endpoints. */
@TestComponent
public class ApiClient {

	public static final String PASSWORD = "S3cure-password!";

	private final MockMvc mvc;

	public ApiClient(MockMvc mvc) {
		this.mvc = mvc;
	}

	public static String uniqueSlug(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	public Tokens signUp(String slug, String email) throws Exception {
		String body = """
				{"companyName": "Acme %1$s", "tenant": "%1$s", "fullName": "Owner of %1$s",
				 "email": "%2$s", "password": "%3$s"}""".formatted(slug, email, PASSWORD);
		String json = mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return Tokens.of(json);
	}

	public record Tokens(String accessToken, String refreshToken) {

		public static Tokens of(String json) {
			return new Tokens(JsonPath.read(json, "$.accessToken"), JsonPath.read(json, "$.refreshToken"));
		}

		public String bearer() {
			return "Bearer " + accessToken;
		}

	}

}
