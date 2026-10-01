package br.com.ferro.model;

import java.util.List;

public record DayRequest(
        Long id,
        Integer weekday,
        String name,
        List<ExerciseRequest> exercises
) {}
