package com.enterprise.asset.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:9000}")
    private String allowedOrigins;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, JwtRequestFilter jwtRequestFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacyCompatiblePasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout",
                                "/api/auth/validate-token").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 用户/角色管理写操作仅限系统管理员
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/roles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/roles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/roles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/departments/**").hasRole("ADMIN")
                        // 使用大写角色名称以匹配 UserDetailsImpl 生成的 ROLE_ 前缀权限
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/leader/**").hasAnyRole("LEADER", "ADMIN")
                        .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/inbound/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/departments/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/suppliers/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/roles/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/asset-applications/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/assets/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/asset-categories/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/purchase-orders/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/purchase-requests/pending/**").hasAnyRole("LEADER", "ADMIN")
                        .requestMatchers("/api/purchase-requests/approved/**").hasAnyRole("LEADER", "ADMIN")
                        .requestMatchers("/api/purchase-requests/*/approve").hasAnyRole("LEADER", "ADMIN")
                        .requestMatchers("/api/purchase-requests/*/reject").hasAnyRole("LEADER", "ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":403,\"message\":\"没有访问权限\",\"data\":null}");
                        }));

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}