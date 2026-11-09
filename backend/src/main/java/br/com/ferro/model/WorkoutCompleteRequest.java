package br.com.ferro.model;

import java.util.List;

public record WorkoutCompleteRequest(
        Long planId,
        Integer weekday,
        List<Long> completedExerciseIds,
        // Opcional (AAAA-MM-DD): data em que o treino foi realizado.
        // Permite registrar um dia anterior da semana atual. Se vier vazio, usa hoje.
        String date,
        // Cargas informadas durante o treino; ausentes mantêm a carga cadastrada no plano.
        List<ExerciseLoadRequest> exerciseLoads
) {}
