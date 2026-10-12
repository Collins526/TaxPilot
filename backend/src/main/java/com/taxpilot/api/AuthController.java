package com.taxpilot.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxpilot.dto.AuthenticationRequest;
import com.taxpilot.dto.AuthenticationResponse;
import com.taxpilot.dto.RefreshRequest;
import com.taxpilot.dto.RegistrationRequest;
import com.taxpilot.service.AuthService;
import com.taxpilot.service.TokenRevocationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenRevocationService tokenRevocationService;

    public AuthController(AuthService authService, TokenRevocationService tokenRevocationService) {
        this.authService = authService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody(required = false) RefreshRequest body) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokenRevocationService.revoke(authorization.substring(7));
        }
        if (body != null && body.refreshToken() != null) {
            tokenRevocationService.revoke(body.refreshToken());
        }
        return ResponseEntity.noContent().build();
    }
}
