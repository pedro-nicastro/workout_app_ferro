package br.com.ferro.model;

import java.util.List;

public record ProgressResponse(
        long totalWorkouts,
        long workoutsThisWeek,
        long workoutsThisMonth,
        long totalExercises,
        double totalVolume,
        int currentStreak,
        List<WeeklyVolume> weeklyVolume,
        List<PersonalRecord> personalRecords
) {
    public record WeeklyVolume(String label, double volume) {}
    public record PersonalRecord(String exercise, double weight, int reps) {}
}
