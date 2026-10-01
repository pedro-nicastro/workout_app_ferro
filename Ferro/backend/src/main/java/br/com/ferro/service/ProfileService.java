package br.com.ferro.service;

import br.com.ferro.model.ProfileUpdateRequest;
import br.com.ferro.model.UserResponse;
import br.com.ferro.repository.FerroRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final FerroRepository repository;
    private final AuthService auth;

    public ProfileService(FerroRepository repository, AuthService auth) {
        this.repository = repository;
        this.auth = auth;
    }

    public UserResponse update(String token, ProfileUpdateRequest request) {
        Long userId = auth.currentUser(token);
        FerroRepository.UserRecord currentUser = repository.updateUser(
                userId, request.name(), request.email()
        );
        return new UserResponse(currentUser.id(), currentUser.name(), currentUser.email());
    }
}
