package com.freelancemusiccrm.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freelancemusiccrm.dto.auth.CsrfTokenResponseDto;
import com.freelancemusiccrm.dto.auth.LoginRequestDto;
import com.freelancemusiccrm.dto.auth.LoginResponseDto;
import com.freelancemusiccrm.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request,
                                                  HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.login(request, httpServletRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest httpServletRequest) {
        boolean wasAuthenticated = authService.logout(httpServletRequest);
        if (!wasAuthenticated) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "認証が必要です"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "ログアウトしました"
        ));
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponseDto> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(new CsrfTokenResponseDto(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName()
        ));
    }

    @GetMapping("/session-expired")
    public ResponseEntity<Map<String, Object>> sessionExpired() {
        return ResponseEntity.status(401).body(Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", "セッションの有効期限が切れました"
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof String principalEmail)) {
            return ResponseEntity.status(401).body(Map.of("message", "認証が必要です"));
        }

        return ResponseEntity.ok(Map.of("email", principalEmail));
    }
}
