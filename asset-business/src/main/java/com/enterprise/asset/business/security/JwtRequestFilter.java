package com.enterprise.asset.business.security;

import com.enterprise.asset.business.client.AuthFeignClient;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.dto.ValidateTokenRequest;
import com.enterprise.asset.common.security.JwtUtil;
import com.enterprise.asset.common.util.Result;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务服务JWT过滤器
 * 优先本地校验签名与claims（避免每个请求都调用auth服务）；
 * 旧版Token缺少uid等claims时，回退到auth服务的validate-token接口。
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final AuthFeignClient authFeignClient;
    private final JwtUtil jwtUtil;

    public JwtRequestFilter(AuthFeignClient authFeignClient, JwtUtil jwtUtil) {
        this.authFeignClient = authFeignClient;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 跳过认证相关接口（这些接口会被转发到auth服务）
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
        }

        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDTO userDTO = authenticateLocally(jwt);
            if (userDTO == null) {
                userDTO = authenticateRemotely(jwt);
            }

            if (userDTO != null) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDTO,
                        null,
                        userDTO.getRoleCodes() != null
                                ? userDTO.getRoleCodes().stream()
                                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                        .collect(Collectors.toList())
                                : Collections.emptyList());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("Authentication set in SecurityContext for user: {}", userDTO.getUsername());
            }
        }

        chain.doFilter(request, response);
    }

    /** 本地解析Token；旧版Token缺少uid时返回 null 以触发远程校验 */
    private UserDTO authenticateLocally(String jwt) {
        try {
            Claims claims = jwtUtil.extractAllClaims(jwt);
            Object uidClaim = claims.get("uid");
            if (uidClaim == null) {
                return null;
            }

            UserDTO userDTO = new UserDTO();
            userDTO.setId(((Number) uidClaim).longValue());
            userDTO.setUsername(claims.getSubject());
            userDTO.setRealName((String) claims.get("realName"));

            Object deptClaim = claims.get("deptId");
            if (deptClaim instanceof Number number) {
                userDTO.setDeptId(number.longValue());
            }

            List<String> roleCodes = new ArrayList<>();
            Object rolesClaim = claims.get("roles");
            if (rolesClaim instanceof List<?> roles) {
                for (Object role : roles) {
                    String code = String.valueOf(role);
                    if (code.startsWith("ROLE_")) {
                        code = code.substring(5);
                    }
                    roleCodes.add(code.toLowerCase());
                }
            }
            userDTO.setRoleCodes(roleCodes);
            return userDTO;
        } catch (Exception e) {
            logger.debug("本地Token校验失败: {}", e.getMessage());
            return null;
        }
    }

    private UserDTO authenticateRemotely(String jwt) {
        try {
            ValidateTokenRequest validateRequest = new ValidateTokenRequest();
            validateRequest.setToken(jwt);

            Result<UserDTO> result = authFeignClient.validateToken(validateRequest);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                return result.getData();
            }
            logger.warn("Token validation failed: {}", result != null ? result.getMessage() : "No response");
        } catch (Exception e) {
            logger.error("Error in JWT filter while calling auth service: {}", e.getMessage());
        }
        return null;
    }
}
