package br.com.ferro.model;

import java.util.List;

public record PlanRequest(
        String name,
        Integer restDays,
        List<DayRequest> days
) {}
