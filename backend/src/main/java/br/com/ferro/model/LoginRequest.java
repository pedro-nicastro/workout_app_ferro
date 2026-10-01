package br.com.ferro.model;

public record LoginRequest(
        String email,
        String password
) {}
