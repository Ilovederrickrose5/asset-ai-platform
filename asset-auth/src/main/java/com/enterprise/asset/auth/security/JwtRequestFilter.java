package com.enterprise.asset.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.enterprise.asset.common.security.JwtUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public JwtRequestFilter(UserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 仅登录/注册/登出/Token校验无需预先认证；其余 /api/auth/** 接口需携带有效 Token
        String path = request.getRequestURI();
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/logout")
                || path.equals("/api/auth/validate-token");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
                logger.debug("JWT Token extracted for username: {}", username);
            } catch (Exception e) {
                logger.error("JWT Token extraction failed: {}", e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    logger.debug("Token validated successfully for user: {}", username);

                    // 从Token中提取角色信息
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    try {
                        Map<String, Object> claims = jwtUtil.extractAllClaims(jwt);
                        Object rolesObj = claims.get("roles");
                        if (rolesObj instanceof List<?>) {
                            List<?> rolesList = (List<?>) rolesObj;
                            for (Object roleObj : rolesList) {
                                if (roleObj instanceof String) {
                                    authorities.add(new SimpleGrantedAuthority((String) roleObj));
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Failed to extract roles from JWT: {}", e.getMessage());
                    }

                    // 如果没有从Token中提取到角色，使用UserDetails中的角色
                    if (authorities.isEmpty()) {
                        logger.debug("Using authorities from UserDetails");
                        authorities = new ArrayList<>(userDetails.getAuthorities());
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Authentication set in SecurityContext for user: {}", username);
                } else {
                    logger.warn("Token validation failed for user: {}", username);
                }
            } catch (Exception e) {
                logger.error("Error in JWT filter for user {}: {}", username, e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}