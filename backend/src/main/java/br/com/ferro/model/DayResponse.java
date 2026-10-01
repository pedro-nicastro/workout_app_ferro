package br.com.ferro.model;

import java.util.List;

public record DayResponse(
        Long id,
        Integer weekday,
        String name,
        List<ExerciseResponse> exercises
) {}
