package br.com.ferro.model;

public record ExerciseResponse(
        Long id,
        String name,
        String sets,
        String reps,
        String weight
) {}
