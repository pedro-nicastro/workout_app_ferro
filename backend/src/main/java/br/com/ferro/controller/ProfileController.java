package br.com.ferro.controller;

import br.com.ferro.model.ProfileUpdateRequest;
import br.com.ferro.model.ProfileSettingsRequest;
import br.com.ferro.model.ProfileSettingsResponse;
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

    @GetMapping("/settings")
    public ProfileSettingsResponse settings(@RequestHeader("Authorization") String auth) {
        return service.settings(token(auth));
    }

    @PutMapping("/settings")
    public ProfileSettingsResponse updateSettings(
            @RequestHeader("Authorization") String auth,
            @RequestBody ProfileSettingsRequest request
    ) {
        return service.updateSettings(token(auth), request);
    }

    private String token(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing token.");
        }
        return auth.substring(7).trim();
    }
}
