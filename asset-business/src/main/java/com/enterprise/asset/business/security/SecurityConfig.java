package com.enterprise.asset.business.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 业务服务安全配置
 * 所有认证逻辑通过Feign调用auth服务完成
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:9000}")
    private String allowedOrigins;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
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
                        // 认证相关接口转发到auth服务，不需要在business服务中验证
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 资产写操作仅限资产管理员/系统管理员
                        .requestMatchers(HttpMethod.POST, "/api/assets/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/assets/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/assets/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/assets/**").hasAnyRole("MANAGER", "ADMIN")
                        // 折旧计算/回滚属于资产管理操作
                        .requestMatchers(HttpMethod.POST, "/api/depreciation/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/depreciation/**").hasAnyRole("MANAGER", "ADMIN")
                        // 折旧记录与报表仅限管理/领导角色
                        .requestMatchers("/api/depreciation/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/reports/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        // 供应商与资产分类的写操作仅限管理角色
                        .requestMatchers(HttpMethod.POST, "/api/suppliers/**", "/api/asset-categories/**")
                        .hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/suppliers/**", "/api/asset-categories/**")
                        .hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/suppliers/**", "/api/asset-categories/**")
                        .hasAnyRole("MANAGER", "ADMIN")
                        // 审批动作仅限拥有审批职责的角色
                        .requestMatchers(HttpMethod.POST, "/api/asset-applications/*/approve")
                        .hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/asset-applications/*/reject")
                        .hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/asset-applications/create-final-approval")
                        .hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        // 用户/角色管理仅限系统管理员
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        // 业务接口权限配置
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/leader/**").hasAnyRole("LEADER", "ADMIN")
                        .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/inbound/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/departments/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/suppliers/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/roles/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/asset-applications/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/assets/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/asset-categories/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/asset-inventory/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/disposal-records/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/purchase-orders/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/purchase-requests/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/dashboard/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
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

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}