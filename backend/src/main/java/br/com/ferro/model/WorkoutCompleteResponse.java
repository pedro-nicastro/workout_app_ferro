package br.com.ferro.model;

public record WorkoutCompleteResponse(
        Long workoutId,
        Integer exercisesTotal,
        String date,
        String dayName,
        String planName,
        String volume
) {}
