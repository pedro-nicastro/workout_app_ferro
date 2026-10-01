package br.com.ferro.service;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SessionService {

    private final ConcurrentMap<String, Long> sessions = new ConcurrentHashMap<>();

    public String create(Long userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);
        return token;
    }

    public Long userId(String token) {
        if (token == null || token.isBlank()) return null;
        return sessions.get(token);
    }

    public void remove(String token) {
        if (token != null) sessions.remove(token);
    }
}
