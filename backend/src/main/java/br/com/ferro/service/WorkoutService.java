package br.com.ferro.service;

import br.com.ferro.model.HistoryItemResponse;
import br.com.ferro.model.ProgressResponse;
import br.com.ferro.model.WorkoutCompleteRequest;
import br.com.ferro.model.WorkoutLoadDraftRequest;
import br.com.ferro.model.WorkoutCompleteResponse;
import br.com.ferro.model.WorkoutDetailResponse;
import br.com.ferro.repository.FerroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Service
public class WorkoutService {

    private final FerroRepository repository;
    private final AuthService auth;

    public WorkoutService(FerroRepository repository, AuthService auth) {
        this.repository = repository;
        this.auth = auth;
    }

    @Transactional
    public WorkoutCompleteResponse complete(String token, WorkoutCompleteRequest request) {
        Long userId = auth.currentUser(token);
        return repository.completeWorkout(userId, request);
    }

    public Map<Long, BigDecimal> loadDrafts(String token, Long planId, Integer weekday, String date) {
        return repository.workoutLoadDrafts(auth.currentUser(token), planId, weekday, date);
    }

    @Transactional
    public void saveLoadDraft(String token, WorkoutLoadDraftRequest request) {
        repository.saveWorkoutLoadDraft(auth.currentUser(token), request);
    }

    public List<HistoryItemResponse> history(String token) {
        return repository.workoutHistory(auth.currentUser(token));
    }

    public WorkoutDetailResponse historyDetail(String token, Long workoutId) {
        return repository.workoutDetail(auth.currentUser(token), workoutId);
    }

    @Transactional
    public void deleteHistoryItem(String token, Long workoutId) {
        repository.deleteWorkout(auth.currentUser(token), workoutId);
    }

    public ProgressResponse progress(String token) {
        return repository.progress(auth.currentUser(token));
    }
}
