package com.feedrelay

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http {
			authorizeHttpRequests {
				authorize("/actuator/health/**", permitAll)
				authorize("/", permitAll)
				authorize("/index.html", permitAll)
				authorize("/assets/**", permitAll)
				authorize("/vite.svg", permitAll)
				authorize(anyRequest, authenticated)
			}
		}
		return http.build()
	}
}
