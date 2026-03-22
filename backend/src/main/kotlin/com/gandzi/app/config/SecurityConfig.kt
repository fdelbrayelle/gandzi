package com.gandzi.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { cors ->
                cors.configurationSource(
                    UrlBasedCorsConfigurationSource().apply {
                        registerCorsConfiguration("/api/**", CorsConfiguration().apply {
                            allowedOrigins = listOf("http://localhost:4321")
                            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                            allowedHeaders = listOf("*")
                            allowCredentials = true
                        })
                    }
                )
            }
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health", "/api/v1/ping").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt { } }
        return http.build()
    }
}
