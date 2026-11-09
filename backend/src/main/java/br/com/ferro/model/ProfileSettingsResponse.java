package br.com.ferro.model;

import java.math.BigDecimal;

public record ProfileSettingsResponse(
        String unit,
        BigDecimal currentWeightKg,
        BigDecimal goalWeightKg,
        boolean dailyWorkoutReminder,
        boolean personalRecordNotification,
        boolean weeklySummaryNotification
) {}
