package br.com.ferro.controller;

import br.com.ferro.model.ProfileUpdateRequest;
import br.com.ferro.model.UserResponse;
import br.com.ferro.service.ProfileService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @PutMapping
    public UserResponse update(
            @RequestHeader("Authorization") String auth,
            @RequestBody ProfileUpdateRequest request
    ) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing token.");
        }
        return service.update(auth.substring(7).trim(), request);
    }
}
