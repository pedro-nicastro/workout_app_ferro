package br.com.ferro.repository;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkoutDraftDateValidationTest {

    private final LocalDate sunday = LocalDate.of(2026, 11, 8);

    @Test
    void acceptsFutureWorkoutDaysWithinTheCurrentWeek() {
        assertEquals(
                LocalDate.of(2026, 11, 9),
                FerroRepository.resolveWorkoutDraftDate("2026-11-09", 1, sunday)
        );
        assertEquals(
                LocalDate.of(2026, 11, 12),
                FerroRepository.resolveWorkoutDraftDate("2026-11-12", 4, sunday)
        );
    }

    @Test
    void rejectsDatesOutsideTheCurrentWeek() {
        assertThrows(IllegalArgumentException.class,
                () -> FerroRepository.resolveWorkoutDraftDate("2026-11-15", 0, sunday));
        assertThrows(IllegalArgumentException.class,
                () -> FerroRepository.resolveWorkoutDraftDate("2026-11-01", 0, sunday));
    }

    @Test
    void rejectsDatesThatDoNotMatchTheSelectedWeekday() {
        assertThrows(IllegalArgumentException.class,
                () -> FerroRepository.resolveWorkoutDraftDate("2026-11-09", 2, sunday));
    }
}
