package br.com.ferro.model;

import java.util.List;

public record WorkoutDetailResponse(
        Long id,
        String planName,
        String dayName,
        String date,
        String volume,
        int totalSets,
        List<ExerciseDetail> exercises
) {
    public record ExerciseDetail(String name, List<SetDetail> sets) {}
    public record SetDetail(int number, int reps, double weight) {}
}
