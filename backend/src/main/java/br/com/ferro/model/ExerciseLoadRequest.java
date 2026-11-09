package br.com.ferro.model;

import java.math.BigDecimal;

public record ExerciseLoadRequest(Long exerciseId, BigDecimal weight) {}
