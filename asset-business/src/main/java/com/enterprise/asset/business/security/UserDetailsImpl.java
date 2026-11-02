package com.enterprise.asset.business.security;

import com.enterprise.asset.business.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class UserDetailsImpl implements UserDetails {

    public UserDetailsImpl(Long id, String username, String email, String password, String realName, Long departmentId,
            String position, Integer status, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.realName = realName;
        this.departmentId = departmentId;
        this.position = position;
        this.status = status;
        this.authorities = authorities;
    }

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private String realName;
    private Long departmentId;
    private String position;
    private Integer status;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl Build(User user) {
        // 从userRoles关联表获取所有角色
        List<GrantedAuthority> authorities = user.getRoleCodes().stream()
                .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
                .collect(Collectors.toList());

        // 如果没有角色,默认设置为普通用户角色
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRealName(),
                user.getDeptId(),
                user.getPosition(),
                user.getStatus(),
                authorities);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}