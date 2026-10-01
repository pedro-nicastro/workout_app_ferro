package br.com.ferro.model;

public record ProfileUpdateRequest(
        String name,
        String email
) {}
