package br.com.ferro.model;

public record UserResponse(
        Long id,
        String name,
        String email
) {}
