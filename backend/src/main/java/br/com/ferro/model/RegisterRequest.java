package br.com.ferro.model;

public record RegisterRequest(
        String name,
        String email,
        String password,
        Boolean termsAccepted
) {}
