package com.hrsaas.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Background jobs; disabled with {@code hrsaas.scheduling.enabled=false} (e.g. in tests). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "hrsaas.scheduling.enabled", matchIfMissing = true)
class SchedulingConfig {

}
