package com.jobskillsmatcher.context.web;

import com.jobskillsmatcher.auth.AuthService;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthService authService,
                                                   @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource,
                                                   ObjectProvider<ClientRegistrationRepository> oauthRegistrations) throws Exception {
        http.addFilterBefore(authService::jwtFilter, LogoutFilter.class);
        http.cors(cors -> cors.configurationSource(corsSource));
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(except -> except
                .authenticationEntryPoint(authService::failure)
                .accessDeniedHandler(authService::failure));
        http.formLogin(AbstractHttpConfigurer::disable);
        if (oauthRegistrations.getIfAvailable() != null) {
            http.oauth2Login(login -> login
                    .successHandler(authService::authenticate)
                    .failureHandler(authService::failure));
        }
        http.logout(logout -> logout.logoutSuccessHandler(authService::logout));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/health",
                        "/api/auth/**",
                        "/actuator/health/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/oauth2/**",
                        "/login/oauth2/**"
                ).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public RSAPrivateKey jwtPrivateKey(@Value("${spring.security.jwt.private}") Resource resource) throws IOException {
        try (var in = resource.getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(in);
        }
    }

    @Bean
    public RSAPublicKey jwtPublicKey(@Value("${spring.security.jwt.public}") Resource resource) throws IOException {
        try (var in = resource.getInputStream()) {
            return RsaKeyConverters.x509().convert(in);
        }
    }

    @Bean
    public JWSSigner jwsSigner(RSAPrivateKey privateKey) {
        return new RSASSASigner(privateKey);
    }

    @Bean
    public RSASSAVerifier rsassaVerifier(RSAPublicKey publicKey) {
        return new RSASSAVerifier(publicKey);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Plain bcrypt (no {id} prefix) — keeps seeded password hashes valid.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${jobskillsmatcher.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
