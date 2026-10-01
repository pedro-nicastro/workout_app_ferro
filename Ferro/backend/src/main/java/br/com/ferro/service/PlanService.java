package br.com.ferro.service;

import br.com.ferro.model.*;
import br.com.ferro.repository.FerroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanService {

    private final FerroRepository repository;
    private final AuthService auth;

    public PlanService(FerroRepository repository, AuthService auth) {
        this.repository = repository;
        this.auth = auth;
    }

    public List<PlanResponse> list(String token) {
        return repository.listPlans(auth.currentUser(token));
    }

    public PlanResponse find(String token, Long id) {
        return repository.findPlan(id, auth.currentUser(token));
    }

    public PlanResponse active(String token) {
        return repository.findActivePlan(auth.currentUser(token));
    }

    @Transactional
    public PlanResponse create(String token, PlanRequest request) {
        Long userId = auth.currentUser(token);
        Long id = repository.createPlan(userId, request);
        return repository.findPlan(id, userId);
    }

    @Transactional
    public PlanResponse update(String token, Long id, PlanRequest request) {
        Long userId = auth.currentUser(token);
        repository.updatePlan(userId, id, request);
        return repository.findPlan(id, userId);
    }

    @Transactional
    public void delete(String token, Long id) {
        repository.deletePlan(auth.currentUser(token), id);
    }

    @Transactional
    public PlanResponse activate(String token, Long id) {
        Long userId = auth.currentUser(token);
        repository.activatePlan(userId, id);
        return repository.findPlan(id, userId);
    }
}
