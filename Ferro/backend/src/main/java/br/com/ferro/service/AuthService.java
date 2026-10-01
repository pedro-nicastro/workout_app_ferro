package br.com.ferro.service;

import br.com.ferro.model.*;
import br.com.ferro.repository.FerroRepository;
import br.com.ferro.security.PasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final FerroRepository repository;
    private final PasswordService passwords;
    private final SessionService sessions;

    public AuthService(FerroRepository repository, PasswordService passwords, SessionService sessions) {
        this.repository = repository;
        this.passwords = passwords;
        this.sessions = sessions;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        String email = normalizeEmail(request.email());
        String password = request.password() == null ? "" : request.password();

        if (name.isBlank() || email.isBlank() || password.length() < 8) {
            throw new IllegalArgumentException("Enter a name, email, and password with at least 8 characters.");
        }

        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            throw new IllegalArgumentException("You must accept the terms of use.");
        }

        if (repository.emailExists(email)) {
            throw new IllegalArgumentException("This email is already registered.");
        }

        Long id = repository.createUser(name, email, passwords.hash(password), true);
        String token = sessions.create(id);

        return new AuthResponse(token, id, name, email);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = request.password() == null ? "" : request.password();

        FerroRepository.UserRecord user = repository.findUserByEmail(email);

        if (user == null || !passwords.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return new AuthResponse(
                sessions.create(user.id()),
                user.id(),
                user.name(),
                user.email()
        );
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    public Long currentUser(String token) {
        Long id = sessions.userId(token);
        if (id == null) throw new IllegalArgumentException("Invalid or expired session.");
        return id;
    }

    public UserResponse me(String token) {
        return repository.findUser(currentUser(token));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
