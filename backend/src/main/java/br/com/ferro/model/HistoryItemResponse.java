package br.com.ferro.model;

import java.util.List;

public record HistoryItemResponse(
        Long id,
        String planName,
        String dayName,
        String date,
        Integer exercises,
        String volume,
        double volumeValue,
        List<String> exerciseNames
) {}
