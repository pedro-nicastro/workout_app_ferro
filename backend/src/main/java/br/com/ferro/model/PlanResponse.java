package br.com.ferro.model;

import java.util.List;

public record PlanResponse(
        Long id,
        String name,
        Integer restDays,
        Boolean active,
        String createdAt,
        String updatedAt,
        List<DayResponse> days
) {}
