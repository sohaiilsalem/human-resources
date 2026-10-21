package com.hrsaas;

import org.springframework.boot.SpringApplication;

public class TestHrSaasApplication {

	public static void main(String[] args) {
		SpringApplication.from(HrSaasApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
