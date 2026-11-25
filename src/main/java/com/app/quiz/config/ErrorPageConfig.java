package com.app.quiz.config;

import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ErrorPageConfig {

	@Bean
	public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> quizErrorPageCustomizer() {
		return factory -> factory.addErrorPages(
			new ErrorPage(HttpStatus.FORBIDDEN, "/403"),
			new ErrorPage(HttpStatus.NOT_FOUND, "/404")
		);
	}
}
