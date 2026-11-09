package br.com.ferro.controller;

import br.com.ferro.model.HistoryItemResponse;
import br.com.ferro.model.ProgressResponse;
import br.com.ferro.model.WorkoutCompleteRequest;
import br.com.ferro.model.WorkoutLoadDraftRequest;
import br.com.ferro.model.WorkoutCompleteResponse;
import br.com.ferro.model.WorkoutDetailResponse;
import br.com.ferro.service.WorkoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService service;

    public WorkoutController(WorkoutService service) {
        this.service = service;
    }

    @PostMapping("/complete")
    public WorkoutCompleteResponse complete(
            @RequestHeader("Authorization") String auth,
            @RequestBody WorkoutCompleteRequest request
    ) {
        return service.complete(token(auth), request);
    }

    @GetMapping("/draft-loads")
    public Map<Long, BigDecimal> loadDrafts(
            @RequestHeader("Authorization") String auth,
            @RequestParam Long planId,
            @RequestParam Integer weekday,
            @RequestParam String date
    ) {
        return service.loadDrafts(token(auth), planId, weekday, date);
    }

    @PutMapping("/draft-loads")
    public ResponseEntity<Void> saveLoadDraft(
            @RequestHeader("Authorization") String auth,
            @RequestBody WorkoutLoadDraftRequest request
    ) {
        service.saveLoadDraft(token(auth), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public List<HistoryItemResponse> history(
            @RequestHeader("Authorization") String auth
    ) {
        return service.history(token(auth));
    }

    @GetMapping("/history/{id}")
    public WorkoutDetailResponse historyDetail(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id
    ) {
        return service.historyDetail(token(auth), id);
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteHistoryItem(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id
    ) {
        service.deleteHistoryItem(token(auth), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/progress")
    public ProgressResponse progress(
            @RequestHeader("Authorization") String auth
    ) {
        return service.progress(token(auth));
    }

    private String token(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing token.");
        }
        return auth.substring(7).trim();
    }
}
