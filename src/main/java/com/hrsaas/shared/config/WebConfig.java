package com.hrsaas.shared.config;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration(proxyBeanMethods = false)
class WebConfig {

	static final Locale ARABIC = Locale.of("ar");

	/** {@code Accept-Language: ar|en} selects message and document language; English otherwise. */
	@Bean
	LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setSupportedLocales(List.of(Locale.ENGLISH, ARABIC));
		resolver.setDefaultLocale(Locale.ENGLISH);
		return resolver;
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
