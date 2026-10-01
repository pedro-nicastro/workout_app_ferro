package br.com.ferro.controller;

import br.com.ferro.model.PlanRequest;
import br.com.ferro.model.PlanResponse;
import br.com.ferro.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService service;

    public PlanController(PlanService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlanResponse> list(@RequestHeader("Authorization") String auth) {
        return service.list(token(auth));
    }

    @GetMapping("/active")
    public ResponseEntity<PlanResponse> active(@RequestHeader("Authorization") String auth) {
        PlanResponse plan = service.active(token(auth));
        return plan == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(plan);
    }

    @GetMapping("/{id}")
    public PlanResponse find(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id
    ) {
        return service.find(token(auth), id);
    }

    @PostMapping
    public PlanResponse create(
            @RequestHeader("Authorization") String auth,
            @RequestBody PlanRequest request
    ) {
        return service.create(token(auth), request);
    }

    @PutMapping("/{id}")
    public PlanResponse update(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @RequestBody PlanRequest request
    ) {
        return service.update(token(auth), id, request);
    }

    @PutMapping("/{id}/activate")
    public PlanResponse activate(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id
    ) {
        return service.activate(token(auth), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id
    ) {
        service.delete(token(auth), id);
        return ResponseEntity.noContent().build();
    }

    private String token(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing token.");
        }
        return auth.substring(7).trim();
    }
}
