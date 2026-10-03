package com.example.demo.security;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    public static final String[] ADMIN_ROLES = {
        "SUPER_ADMIN", "ADMIN", "KYC_OFFICER", "SUPPORT", "RISK_OFFICER", "FINANCE", "COMPLIANCE_OFFICER"
    };

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Value("${app.cors.allowed-origins:http://localhost:8081,http://127.0.0.1:5500}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"success\":false,\"message\":\"Full authentication is required to access this resource\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"success\":false,\"message\":\"Access denied\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // 1. permit OPTIONS, static files and public .html pages (including /admin/*.html, which contain no data) BEFORE any role rules, so pages can still load.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/",
                    "/*.html",
                    "/admin/*.html",
                    "/css/**",
                    "/js/**",
                    "/assets/**",
                    "/h2-console/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()

                // 2. permit /auth/** and GET on the public stock catalog (/stocks/**). Note: /auth/me requires authentication
                .requestMatchers("/auth/me", "/api/auth/me").authenticated()
                .requestMatchers("/auth/**", "/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/stocks/**", "/api/stocks/**").permitAll()

                // 3. POST/PUT/DELETE on /stocks/** only for admin roles.
                .requestMatchers(HttpMethod.POST, "/stocks/**", "/api/stocks/**").hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.PUT, "/stocks/**", "/api/stocks/**").hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/stocks/**", "/api/stocks/**").hasAnyRole(ADMIN_ROLES)

                // 4. /admin/** and /api/admin/** (API calls) for the explicit admin roles list.
                .requestMatchers("/admin/**", "/api/admin/**").hasAnyRole(ADMIN_ROLES)

                // 5. /trade/**, /portfolio/**, /watchlist/**, /auth/me and everything else: authenticated.
                .requestMatchers(
                    "/trade/**", "/api/trade/**",
                    "/portfolio/**", "/api/portfolio/**",
                    "/watchlist/**", "/api/watchlist/**"
                ).authenticated()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}