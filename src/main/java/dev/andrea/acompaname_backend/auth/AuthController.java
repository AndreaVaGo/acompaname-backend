package dev.andrea.acompaname_backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.andrea.acompaname_backend.security.SecurityUser;

@RestController
@RequestMapping(path = "${api-endpoint}")
public class AuthController {

    @GetMapping("/login")
    public ResponseEntity<AuthDTOResponse> login() {
        SecurityContext contextHolder = SecurityContextHolder.getContext();
        Authentication auth = contextHolder.getAuthentication();

        SecurityUser securityUser = (SecurityUser) auth.getPrincipal();
        Long id = securityUser.getUsuario().getId();

        AuthDTOResponse authResponse = new AuthDTOResponse(id, "Logged", auth.getName(),
                auth.getAuthorities().iterator().next().getAuthority());
        return ResponseEntity.ok().body(authResponse);
    }

}