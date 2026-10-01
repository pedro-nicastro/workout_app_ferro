package br.com.ferro.model;

public record AuthResponse(
        String token,
        Long id,
        String name,
        String email
) {}
