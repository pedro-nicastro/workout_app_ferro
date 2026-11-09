package br.com.ferro.model;

import java.math.BigDecimal;

public record WorkoutLoadDraftRequest(
        Long planId,
        Integer weekday,
        String date,
        Long exerciseId,
        BigDecimal weight
) {}
