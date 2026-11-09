package br.com.ferro.model;

import java.math.BigDecimal;

public record ProfileSettingsRequest(
        String unit,
        BigDecimal currentWeightKg,
        BigDecimal goalWeightKg,
        Boolean dailyWorkoutReminder,
        Boolean personalRecordNotification,
        Boolean weeklySummaryNotification
) {}
