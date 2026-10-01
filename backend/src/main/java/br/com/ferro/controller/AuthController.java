package br.com.ferro.controller;

import br.com.ferro.model.*;
import br.com.ferro.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8081") 
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.me(token(auth));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        service.logout(optionalToken(auth));
        return ResponseEntity.noContent().build();
    }

    private String token(String auth) {
        String token = optionalToken(auth);
        if (token == null) throw new IllegalArgumentException("Missing token.");
        return token;
    }

    private String optionalToken(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return auth.substring(7).trim();
    }
}
