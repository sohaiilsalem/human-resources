package com.hrsaas;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import com.hrsaas.billing.PayrollProbeController;

/** Full application against a Testcontainers Postgres; one context (and container) shared by all such tests. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = "hrsaas.scheduling.enabled=false")
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, ApiClient.class, PayrollProbeController.class })
public @interface IntegrationTest {

}
