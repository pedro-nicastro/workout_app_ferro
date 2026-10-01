package br.com.ferro.model;

public record ExerciseRequest(
        Long id,
        String name,
        String sets,
        String reps,
        String weight
) {}
