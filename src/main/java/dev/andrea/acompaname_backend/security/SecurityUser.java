package dev.andrea.acompaname_backend.security;

import java.util.ArrayList;
import java.util.Collection;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;

public class SecurityUser implements UserDetails {

    private UsuarioEntity usuario;

    public SecurityUser(UsuarioEntity usuario) {
        this.usuario = usuario;

    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public @Nullable String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (RoleEntity role : usuario.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        }
        return authorities;
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
        return true;
    }

}
