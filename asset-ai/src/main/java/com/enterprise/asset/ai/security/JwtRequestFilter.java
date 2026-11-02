package com.enterprise.asset.ai.security;

import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.security.JwtUtil;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 服务 JWT 过滤器：与业务服务一致，本地校验主系统签发的 Token，
 * 从 uid / roles / deptId 声明构造当前用户，不查询用户表。
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final JwtUtil jwtUtil;

    public JwtRequestFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * SSE 响应完成后 Spring Security 会对 ASYNC 分发重新做一次授权校验，
     * 默认跳过 ASYNC 会导致上下文为空、抛 AccessDenied 并截断连接。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDTO user = authenticateLocally(authorizationHeader.substring(7));
            if (user != null) {
                List<SimpleGrantedAuthority> authorities = user.getRoleCodes() == null
                        ? List.of()
                        : user.getRoleCodes().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("Authentication set for user: {}", user.getUsername());
            }
        }

        chain.doFilter(request, response);
    }

    private UserDTO authenticateLocally(String jwt) {
        try {
            Claims claims = jwtUtil.extractAllClaims(jwt);
            Object uidClaim = claims.get("uid");
            if (uidClaim == null) {
                logger.warn("Token missing uid claim, request rejected");
                return null;
            }

            UserDTO user = new UserDTO();
            user.setId(((Number) uidClaim).longValue());
            user.setUsername(claims.getSubject());
            user.setRealName((String) claims.get("realName"));

            Object deptClaim = claims.get("deptId");
            if (deptClaim instanceof Number number) {
                user.setDeptId(number.longValue());
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
            user.setRoleCodes(roleCodes);

            return user;
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return null;
        }
    }
}
