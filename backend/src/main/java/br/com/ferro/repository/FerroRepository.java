package br.com.ferro.repository;

import br.com.ferro.model.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Locale;
import java.util.Objects;

@Repository
public class FerroRepository {

    private final JdbcTemplate jdbc;

    public FerroRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean emailExists(String email) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE email = ?",
                Long.class, email
        );
        return total != null && total > 0;
    }

    public Long createUser(String name, String email, String passwordHash, boolean termsAccepted) {
        KeyHolder keys = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO usuarios (nome, email, senha_hash, termos_aceitos) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setBoolean(4, termsAccepted);
            return ps;
        }, keys);

        Number key = Objects.requireNonNull(keys.getKey(), "User ID was not returned.");
        Long id = key.longValue();

        jdbc.update("INSERT INTO perfis_usuario (usuario_id) VALUES (?)", id);
        return id;
    }

    public UserRecord findUserByEmail(String email) {
        List<UserRecord> result = jdbc.query(
                "SELECT id, nome, email, senha_hash FROM usuarios WHERE email = ? LIMIT 1",
                (rs, rowNum) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("senha_hash")
                ),
                email
        );
        return result.isEmpty() ? null : result.get(0);
    }

    public UserResponse findUser(Long id) {
        return jdbc.queryForObject(
                "SELECT id, nome, email FROM usuarios WHERE id = ?",
                (rs, rowNum) -> new UserResponse(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getString("email")
                ),
                id
        );
    }

    public List<PlanResponse> listPlans(Long userId) {
        List<Long> ids = jdbc.query(
                "SELECT id FROM planos WHERE usuario_id = ? ORDER BY ativo DESC, criado_em DESC, id DESC",
                (rs, rowNum) -> rs.getLong("id"),
                userId
        );

        List<PlanResponse> result = new ArrayList<>();
        for (Long id : ids) {
            result.add(findPlan(id, userId));
        }
        return result;
    }

    public PlanResponse findPlan(Long planId, Long userId) {
        PlanRow p = jdbc.queryForObject(
                "SELECT id, nome, dias_descanso, ativo, criado_em, atualizado_em " +
                "FROM planos WHERE id = ? AND usuario_id = ?",
                (rs, rowNum) -> new PlanRow(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getInt("dias_descanso"),
                        rs.getBoolean("ativo"),
                        rs.getTimestamp("criado_em"),
                        rs.getTimestamp("atualizado_em")
                ),
                planId, userId
        );

        List<DayResponse> days = jdbc.query(
                "SELECT id, dia_semana, nome FROM plano_dias WHERE plano_id = ? ORDER BY dia_semana, id",
                (rs, rowNum) -> new DayResponse(
                        rs.getLong("id"),
                        rs.getInt("dia_semana"),
                        rs.getString("nome"),
                        listExercises(rs.getLong("id"))
                ),
                planId
        );

        return new PlanResponse(
                p.id, p.name, p.restDays, p.active,
                p.createdAt == null ? null : p.createdAt.toLocalDateTime().toString(),
                p.updatedAt == null ? null : p.updatedAt.toLocalDateTime().toString(),
                days
        );
    }

    private List<ExerciseResponse> listExercises(Long dayId) {
        return jdbc.query(
                "SELECT id, nome, series, repeticoes, carga " +
                "FROM exercicios WHERE plano_dia_id = ? ORDER BY ordem, id",
                (rs, rowNum) -> new ExerciseResponse(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getString("series"),
                        rs.getString("repeticoes"),
                        formatWeight(rs.getBigDecimal("carga"))
                ),
                dayId
        );
    }

    private String formatWeight(BigDecimal weight) {
        if (weight == null) return "";
        return weight.stripTrailingZeros().toPlainString() + "kg";
    }

    public Long createPlan(Long userId, PlanRequest request) {
        return updateOrCreatePlan(userId, null, request, true);
    }

    public void updatePlan(Long userId, Long planId, PlanRequest request) {
        updateOrCreatePlan(userId, planId, request, false);
    }

    private Long updateOrCreatePlan(Long userId, Long planId, PlanRequest request, boolean isNew) {
        Long id = planId;

        if (isNew) {
            jdbc.update("UPDATE planos SET ativo = 0 WHERE usuario_id = ?", userId);

            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO planos (usuario_id, nome, dias_descanso, ativo) VALUES (?, ?, ?, 1)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, userId);
                ps.setString(2, normalizeName(request.name(), "Workout without a name"));
                ps.setInt(3, limitRestDays(request.restDays()));
                return ps;
            }, keys);

            Number key = Objects.requireNonNull(keys.getKey(), "Plan ID was not returned.");
            id = key.longValue();
        } else {
            verifyPlanOwner(planId, userId);
            jdbc.update(
                    "UPDATE planos SET nome = ?, dias_descanso = ? WHERE id = ? AND usuario_id = ?",
                    normalizeName(request.name(), "Workout without a name"),
                    limitRestDays(request.restDays()),
                    planId, userId
            );
            jdbc.update("DELETE FROM plano_dias WHERE plano_id = ?", planId);
        }

        final Long finalPlanId = id;
        List<DayRequest> days = request.days() == null ? Collections.emptyList() : request.days();
        int calculatedRestDays = 7 - Math.min(7, days.size());

        // A quantidade de descanso é derivada dos dias escolhidos.
        // Assim o banco não fica com uma contagem diferente da seleção semanal.
        jdbc.update(
                "UPDATE planos SET dias_descanso = ? WHERE id = ? AND usuario_id = ?",
                calculatedRestDays, finalPlanId, userId
        );

        int dayOrder = 0;
        boolean[] usedDays = new boolean[7];

        for (DayRequest day : days) {
            if (dayOrder >= 7) break;

            int weekdayValue = day.weekday() == null ? dayOrder : day.weekday();
            if (weekdayValue < 0 || weekdayValue > 6) {
                throw new IllegalArgumentException("Weekday must be between 0 (Sunday) and 6 (Saturday).");
            }
            if (usedDays[weekdayValue]) {
                throw new IllegalArgumentException("The same weekday cannot be added twice.");
            }
            usedDays[weekdayValue] = true;

            KeyHolder dayKey = new GeneratedKeyHolder();
            int currentOrder = dayOrder;

            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO plano_dias (plano_id, dia_semana, nome, ordem) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, finalPlanId);
                ps.setInt(2, weekdayValue);
                ps.setString(3, normalizeName(day.name(), "Day " + (char) ('A' + currentOrder)));
                ps.setInt(4, currentOrder);
                return ps;
            }, dayKey);

            Long dayId = Objects.requireNonNull(dayKey.getKey(), "Day ID was not returned.").longValue();

            List<ExerciseRequest> exercises =
                    day.exercises() == null ? Collections.emptyList() : day.exercises();

            int exerciseOrder = 0;
            for (ExerciseRequest ex : exercises) {
                if (ex.name() == null || ex.name().isBlank()) continue;

                jdbc.update(
                        "INSERT INTO exercicios " +
                        "(plano_dia_id, nome, series, repeticoes, carga, ordem) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                        dayId,
                        ex.name().trim(),
                        emptyToNull(ex.sets()),
                        emptyToNull(ex.reps()),
                        parseWeight(ex.weight()),
                        exerciseOrder++
                );
            }

            dayOrder++;
        }

        return id;
    }

    public void deletePlan(Long userId, Long planId) {
        verifyPlanOwner(planId, userId);
        boolean wasActive = Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT ativo FROM planos WHERE id = ?", Boolean.class, planId)
        );

        jdbc.update("DELETE FROM planos WHERE id = ? AND usuario_id = ?", planId, userId);

        if (wasActive) {
            List<Long> remainingPlans = jdbc.query(
                    "SELECT id FROM planos WHERE usuario_id = ? ORDER BY criado_em DESC, id DESC LIMIT 1",
                    (rs, rowNum) -> rs.getLong("id"),
                    userId
            );
            if (!remainingPlans.isEmpty()) {
                jdbc.update("UPDATE planos SET ativo = 1 WHERE id = ?", remainingPlans.get(0));
            }
        }
    }

    public void activatePlan(Long userId, Long planId) {
        verifyPlanOwner(planId, userId);
        jdbc.update("UPDATE planos SET ativo = 0 WHERE usuario_id = ?", userId);
        jdbc.update("UPDATE planos SET ativo = 1 WHERE id = ? AND usuario_id = ?", planId, userId);
    }

    public PlanResponse findActivePlan(Long userId) {
        List<Long> ids = jdbc.query(
                "SELECT id FROM planos WHERE usuario_id = ? AND ativo = 1 ORDER BY atualizado_em DESC, id DESC LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"),
                userId
        );
        if (ids.isEmpty()) return null;
        return findPlan(ids.get(0), userId);
    }

    public UserRecord updateUser(Long userId, String name, String email) {
        jdbc.update(
                "UPDATE usuarios SET nome = ?, email = ? WHERE id = ?",
                normalizeName(name, "User"),
                email.trim().toLowerCase(Locale.ROOT),
                userId
        );
        return jdbc.queryForObject(
                "SELECT id, nome, email, senha_hash FROM usuarios WHERE id = ?",
                (rs, rowNum) -> new UserRecord(
                        rs.getLong("id"), rs.getString("nome"),
                        rs.getString("email"), rs.getString("senha_hash")
                ),
                userId
        );
    }

    public ProfileSettingsResponse profileSettings(Long userId) {
        ensureProfileSettingsRow(userId);
        return jdbc.queryForObject(
                "SELECT unidade, peso_atual, meta_peso, notificacao_lembrete, " +
                "notificacao_recorde, notificacao_resumo FROM perfis_usuario WHERE usuario_id = ?",
                (rs, rowNum) -> new ProfileSettingsResponse(
                        rs.getString("unidade"),
                        rs.getBigDecimal("peso_atual"),
                        rs.getBigDecimal("meta_peso"),
                        rs.getBoolean("notificacao_lembrete"),
                        rs.getBoolean("notificacao_recorde"),
                        rs.getBoolean("notificacao_resumo")
                ),
                userId
        );
    }

    public ProfileSettingsResponse updateProfileSettings(Long userId, ProfileSettingsRequest request) {
        if (request == null) throw new IllegalArgumentException("Settings are required.");

        String unit = request.unit() == null ? "kg" : request.unit().trim().toLowerCase(Locale.ROOT);
        if (!unit.equals("kg") && !unit.equals("lb")) {
            throw new IllegalArgumentException("Unit must be kg or lb.");
        }
        validateProfileWeight(request.currentWeightKg());
        validateProfileWeight(request.goalWeightKg());

        ensureProfileSettingsRow(userId);
        jdbc.update(
                "UPDATE perfis_usuario SET unidade = ?, peso_atual = ?, meta_peso = ?, " +
                "notificacao_lembrete = ?, notificacao_recorde = ?, notificacao_resumo = ? " +
                "WHERE usuario_id = ?",
                unit,
                request.currentWeightKg(),
                request.goalWeightKg(),
                Boolean.TRUE.equals(request.dailyWorkoutReminder()),
                Boolean.TRUE.equals(request.personalRecordNotification()),
                Boolean.TRUE.equals(request.weeklySummaryNotification()),
                userId
        );
        return profileSettings(userId);
    }

    private void ensureProfileSettingsRow(Long userId) {
        jdbc.update("INSERT IGNORE INTO perfis_usuario (usuario_id) VALUES (?)", userId);
    }

    private void validateProfileWeight(BigDecimal weightKg) {
        if (weightKg != null && (weightKg.signum() < 0 || weightKg.compareTo(new BigDecimal("9999.99")) > 0)) {
            throw new IllegalArgumentException("Profile weights must be between 0 and 9999.99 kg.");
        }
    }

    private void verifyPlanOwner(Long planId, Long userId) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM planos WHERE id = ? AND usuario_id = ?",
                Integer.class, planId, userId
        );
        if (total == null || total == 0) {
            throw new IllegalArgumentException("Plan not found for this user.");
        }
    }

    private int limitRestDays(Integer value) {
        if (value == null) return 0;
        return Math.max(0, Math.min(7, value));
    }

    private String normalizeName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal parseWeight(String weight) {
        if (weight == null || weight.isBlank()) return null;

        String value = weight.toLowerCase(Locale.ROOT)
                .replace("kg", "")
                .replace(",", ".")
                .trim();

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Define quando o treino foi realizado.
     * - Sem data: agora.
     * - Com data (AAAA-MM-DD): precisa ser hoje ou um dia anterior da semana atual
     *   (domingo a sábado) e cair no dia da semana do treino escolhido.
     *   Para dias anteriores, o horário gravado é 12:00.
     */
    private java.time.LocalDateTime resolvePerformedAt(String dateText, int weekday) {
        java.time.LocalDate today = java.time.LocalDate.now();
        if (dateText == null || dateText.isBlank()) {
            return java.time.LocalDateTime.now();
        }

        java.time.LocalDate date;
        try {
            date = java.time.LocalDate.parse(dateText.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date. Use the format YYYY-MM-DD.");
        }

        if (date.isAfter(today)) {
            throw new IllegalArgumentException("You cannot complete a workout in the future.");
        }

        // Semana começa no domingo (0), igual ao restante do FERRO.
        java.time.LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() % 7);
        if (date.isBefore(weekStart)) {
            throw new IllegalArgumentException("Only workouts from the current week can be completed.");
        }

        if (date.getDayOfWeek().getValue() % 7 != weekday) {
            throw new IllegalArgumentException("The date does not match the selected weekday.");
        }

        return date.equals(today)
                ? java.time.LocalDateTime.now()
                : date.atTime(12, 0);
    }

    public Map<Long, BigDecimal> workoutLoadDrafts(Long userId, Long planId, Integer weekday, String dateText) {
        validateWorkoutDraftContext(userId, planId, weekday, dateText);
        java.time.LocalDate workoutDate = java.time.LocalDate.parse(dateText.trim());
        Map<Long, BigDecimal> loads = new HashMap<>();
        jdbc.query(
                "SELECT exercise_id, carga FROM treino_cargas_rascunho " +
                "WHERE usuario_id = ? AND plano_id = ? AND dia_semana = ? AND data_treino = ?",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    loads.put(rs.getLong("exercise_id"), rs.getBigDecimal("carga"));
                },
                userId, planId, weekday, java.sql.Date.valueOf(workoutDate)
        );
        return loads;
    }

    public void saveWorkoutLoadDraft(Long userId, WorkoutLoadDraftRequest request) {
        if (request == null || request.planId() == null || request.exerciseId() == null) {
            throw new IllegalArgumentException("Plan and exercise are required.");
        }
        validateWorkoutDraftContext(userId, request.planId(), request.weekday(), request.date());
        BigDecimal weight = request.weight();
        if (weight == null || weight.signum() < 0 || weight.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new IllegalArgumentException("Exercise loads must be between 0 and 999999.99 kg.");
        }

        Integer belongsToDay = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exercicios e " +
                "INNER JOIN plano_dias pd ON pd.id = e.plano_dia_id " +
                "WHERE e.id = ? AND pd.plano_id = ? AND pd.dia_semana = ?",
                Integer.class, request.exerciseId(), request.planId(), request.weekday()
        );
        if (belongsToDay == null || belongsToDay == 0) {
            throw new IllegalArgumentException("The exercise does not belong to this workout day.");
        }

        jdbc.update(
                "INSERT INTO treino_cargas_rascunho " +
                "(usuario_id, plano_id, dia_semana, data_treino, exercise_id, carga) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE carga = VALUES(carga), atualizado_em = CURRENT_TIMESTAMP",
                userId, request.planId(), request.weekday(),
                java.sql.Date.valueOf(java.time.LocalDate.parse(request.date().trim())),
                request.exerciseId(), weight
        );
    }

    private void validateWorkoutDraftContext(Long userId, Long planId, Integer weekday, String dateText) {
        if (planId == null) throw new IllegalArgumentException("A workout plan is required.");
        verifyPlanOwner(planId, userId);
        if (weekday == null || weekday < 0 || weekday > 6) {
            throw new IllegalArgumentException("Weekday must be between 0 (Sunday) and 6 (Saturday).");
        }
        if (dateText == null || dateText.isBlank()) {
            throw new IllegalArgumentException("Workout date is required.");
        }
        resolveWorkoutDraftDate(dateText, weekday, java.time.LocalDate.now());
        Integer dayExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM plano_dias WHERE plano_id = ? AND dia_semana = ?",
                Integer.class, planId, weekday
        );
        if (dayExists == null || dayExists == 0) {
            throw new IllegalArgumentException("No workout exists for the selected weekday.");
        }
    }

    static java.time.LocalDate resolveWorkoutDraftDate(
            String dateText, int weekday, java.time.LocalDate today
    ) {
        if (dateText == null || dateText.isBlank()) {
            throw new IllegalArgumentException("Workout date is required.");
        }

        java.time.LocalDate date;
        try {
            date = java.time.LocalDate.parse(dateText.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date. Use the format YYYY-MM-DD.");
        }

        // Drafts can be prepared for any day in the current Sunday-to-Saturday week.
        java.time.LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() % 7);
        java.time.LocalDate weekEnd = weekStart.plusDays(6);
        if (date.isBefore(weekStart) || date.isAfter(weekEnd)) {
            throw new IllegalArgumentException("Workout drafts must belong to the current week.");
        }
        if (date.getDayOfWeek().getValue() % 7 != weekday) {
            throw new IllegalArgumentException("The date does not match the selected weekday.");
        }
        return date;
    }

    public WorkoutCompleteResponse completeWorkout(Long userId, WorkoutCompleteRequest request) {
        Long planId = request.planId();
        if (planId == null) throw new IllegalArgumentException("A workout plan is required.");
        verifyPlanOwner(planId, userId);

        Integer weekday = request.weekday();
        if (weekday == null || weekday < 0 || weekday > 6) {
            throw new IllegalArgumentException("Weekday must be between 0 (Sunday) and 6 (Saturday).");
        }

        // Data em que o treino foi feito: hoje (padrão) ou um dia anterior da semana atual.
        final java.time.LocalDateTime performedAt = resolvePerformedAt(request.date(), weekday);

        PlanDaySnapshot day = jdbc.queryForObject(
                "SELECT pd.id, pd.nome, p.nome AS plano_nome " +
                "FROM plano_dias pd INNER JOIN planos p ON p.id = pd.plano_id " +
                "WHERE pd.plano_id = ? AND pd.dia_semana = ?",
                (rs, rowNum) -> new PlanDaySnapshot(
                        rs.getLong("id"), rs.getString("nome"), rs.getString("plano_nome")
                ),
                planId, weekday
        );

        if (day == null) throw new IllegalArgumentException("No workout exists for the selected weekday.");

        List<Long> completedIds = request.completedExerciseIds() == null
                ? Collections.emptyList() : request.completedExerciseIds();

        List<ExerciseSnapshot> exercises = jdbc.query(
                "SELECT id, nome, series, repeticoes, carga, ordem " +
                "FROM exercicios WHERE plano_dia_id = ? ORDER BY ordem, id",
                (rs, rowNum) -> new ExerciseSnapshot(
                        rs.getLong("id"), rs.getString("nome"), rs.getString("series"),
                        rs.getString("repeticoes"), rs.getBigDecimal("carga"), rs.getInt("ordem")
                ), day.id
        );

        if (exercises.isEmpty()) {
            throw new IllegalArgumentException("This workout has no exercises.");
        }

        Map<Long, BigDecimal> submittedLoads = new HashMap<>();
        if (request.exerciseLoads() != null) {
            Set<Long> dayExerciseIds = exercises.stream()
                    .map(ExerciseSnapshot::id)
                    .collect(Collectors.toSet());
            for (ExerciseLoadRequest load : request.exerciseLoads()) {
                if (load == null || load.exerciseId() == null || !dayExerciseIds.contains(load.exerciseId())) {
                    throw new IllegalArgumentException("An exercise load does not belong to this workout.");
                }
                if (load.weight() == null || load.weight().signum() < 0
                        || load.weight().compareTo(new BigDecimal("999999.99")) > 0) {
                    throw new IllegalArgumentException("Exercise loads must be between 0 and 999999.99 kg.");
                }
                submittedLoads.put(load.exerciseId(), load.weight());
            }
        }

        int completedCount = 0;
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (ExerciseSnapshot exercise : exercises) {
            if (!completedIds.contains(exercise.id)) continue;
            completedCount++;
            int sets = parseFirstInt(exercise.sets, 1);
            int reps = parseFirstInt(exercise.reps, 0);
            BigDecimal weight = submittedLoads.getOrDefault(
                    exercise.id, exercise.weight == null ? BigDecimal.ZERO : exercise.weight
            );
            totalVolume = totalVolume.add(weight.multiply(BigDecimal.valueOf((long) sets * reps)));
        }

        if (completedCount == 0) {
            throw new IllegalArgumentException("Complete at least one exercise before finishing the workout.");
        }

        // A lambda só pode usar variáveis que não mudam depois de criadas.
        final int completedCountFinal = completedCount;
        final BigDecimal totalVolumeFinal = totalVolume;

        KeyHolder workoutKey = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO treinos_realizados " +
                    "(usuario_id, plano_id, plano_nome_snapshot, dia_nome_snapshot, exercicios_total, volume_total, realizado_em) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setLong(2, planId);
            ps.setString(3, day.planName);
            ps.setString(4, day.dayName);
            ps.setInt(5, completedCountFinal);
            ps.setBigDecimal(6, totalVolumeFinal);
            ps.setTimestamp(7, Timestamp.valueOf(performedAt));
            return ps;
        }, workoutKey);

        Long workoutId = Objects.requireNonNull(workoutKey.getKey(), "Workout ID was not returned.").longValue();

        for (ExerciseSnapshot exercise : exercises) {
            if (!completedIds.contains(exercise.id)) continue;

            KeyHolder exerciseKey = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO exercicios_realizados " +
                        "(treino_id, exercicio_id, exercicio_nome_snapshot, ordem) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, workoutId);
                ps.setLong(2, exercise.id);
                ps.setString(3, exercise.name);
                ps.setInt(4, exercise.order);
                return ps;
            }, exerciseKey);

            Long performedExerciseId = Objects.requireNonNull(
                    exerciseKey.getKey(), "Performed exercise ID was not returned."
            ).longValue();

            int sets = parseFirstInt(exercise.sets, 1);
            int reps = parseFirstInt(exercise.reps, 0);
            BigDecimal weight = submittedLoads.getOrDefault(
                    exercise.id, exercise.weight == null ? BigDecimal.ZERO : exercise.weight
            );

            for (int set = 1; set <= sets; set++) {
                jdbc.update(
                        "INSERT INTO series_realizadas " +
                        "(exercicio_realizado_id, numero_serie, repeticoes, carga) VALUES (?, ?, ?, ?)",
                        performedExerciseId, set, reps, weight
                );
            }
        }

        jdbc.update(
                "DELETE FROM treino_cargas_rascunho WHERE usuario_id = ? AND plano_id = ? " +
                "AND dia_semana = ? AND data_treino = ?",
                userId, planId, weekday, java.sql.Date.valueOf(performedAt.toLocalDate())
        );

        return new WorkoutCompleteResponse(
                workoutId,
                completedCount,
                performedAt.toString(),
                day.dayName,
                day.planName,
                totalVolume.stripTrailingZeros().toPlainString() + "kg"
        );
    }

    public List<HistoryItemResponse> workoutHistory(Long userId) {
        // Nomes dos exercícios de cada treino (usados na busca da tela History).
        java.util.Map<Long, List<String>> namesByWorkout = new java.util.HashMap<>();
        jdbc.query(
                "SELECT er.treino_id, er.exercicio_nome_snapshot " +
                "FROM exercicios_realizados er " +
                "INNER JOIN treinos_realizados tr ON tr.id = er.treino_id " +
                "WHERE tr.usuario_id = ? ORDER BY er.treino_id, er.ordem, er.id",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    namesByWorkout
                            .computeIfAbsent(rs.getLong("treino_id"), key -> new ArrayList<>())
                            .add(rs.getString("exercicio_nome_snapshot"));
                },
                userId
        );

        return jdbc.query(
                "SELECT id, plano_nome_snapshot, dia_nome_snapshot, realizado_em, exercicios_total, volume_total " +
                "FROM treinos_realizados WHERE usuario_id = ? ORDER BY realizado_em DESC, id DESC LIMIT 500",
                (rs, rowNum) -> {
                    Long id = rs.getLong("id");
                    BigDecimal volume = rs.getBigDecimal("volume_total");
                    return new HistoryItemResponse(
                            id,
                            rs.getString("plano_nome_snapshot"),
                            rs.getString("dia_nome_snapshot"),
                            rs.getTimestamp("realizado_em").toLocalDateTime().toString(),
                            rs.getInt("exercicios_total"),
                            formatWeight(volume),
                            volume == null ? 0.0 : volume.doubleValue(),
                            namesByWorkout.getOrDefault(id, Collections.<String>emptyList())
                    );
                },
                userId
        );
    }

    public WorkoutDetailResponse workoutDetail(Long userId, Long workoutId) {
        List<String[]> header = jdbc.query(
                "SELECT plano_nome_snapshot, dia_nome_snapshot, realizado_em, volume_total " +
                "FROM treinos_realizados WHERE id = ? AND usuario_id = ?",
                (rs, rowNum) -> new String[] {
                        rs.getString("plano_nome_snapshot"),
                        rs.getString("dia_nome_snapshot"),
                        rs.getTimestamp("realizado_em").toLocalDateTime().toString(),
                        formatWeight(rs.getBigDecimal("volume_total"))
                },
                workoutId, userId
        );
        if (header.isEmpty()) {
            throw new IllegalArgumentException("Workout not found for this user.");
        }

        java.util.Map<Long, String> names = new java.util.LinkedHashMap<>();
        java.util.Map<Long, List<WorkoutDetailResponse.SetDetail>> setsByExercise = new java.util.HashMap<>();

        jdbc.query(
                "SELECT er.id AS exercicio_id, er.exercicio_nome_snapshot, " +
                "sr.numero_serie, sr.repeticoes, sr.carga " +
                "FROM exercicios_realizados er " +
                "LEFT JOIN series_realizadas sr ON sr.exercicio_realizado_id = er.id " +
                "WHERE er.treino_id = ? ORDER BY er.ordem, er.id, sr.numero_serie",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    long exerciseId = rs.getLong("exercicio_id");
                    names.putIfAbsent(exerciseId, rs.getString("exercicio_nome_snapshot"));
                    List<WorkoutDetailResponse.SetDetail> sets =
                            setsByExercise.computeIfAbsent(exerciseId, key -> new ArrayList<>());

                    int number = rs.getInt("numero_serie");
                    if (!rs.wasNull()) {
                        sets.add(new WorkoutDetailResponse.SetDetail(
                                number,
                                rs.getInt("repeticoes"),
                                rs.getBigDecimal("carga").doubleValue()
                        ));
                    }
                },
                workoutId
        );

        List<WorkoutDetailResponse.ExerciseDetail> exercises = new ArrayList<>();
        int totalSets = 0;
        for (java.util.Map.Entry<Long, String> entry : names.entrySet()) {
            List<WorkoutDetailResponse.SetDetail> sets = setsByExercise.get(entry.getKey());
            totalSets += sets.size();
            exercises.add(new WorkoutDetailResponse.ExerciseDetail(entry.getValue(), sets));
        }

        String[] row = header.get(0);
        return new WorkoutDetailResponse(workoutId, row[0], row[1], row[2], row[3], totalSets, exercises);
    }

    public void deleteWorkout(Long userId, Long workoutId) {
        // exercicios_realizados e series_realizadas são apagados em cascata pelo banco.
        int removed = jdbc.update(
                "DELETE FROM treinos_realizados WHERE id = ? AND usuario_id = ?",
                workoutId, userId
        );
        if (removed == 0) {
            throw new IllegalArgumentException("Workout not found for this user.");
        }
    }

    public ProgressResponse progress(Long userId) {
        Long totalWorkouts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM treinos_realizados WHERE usuario_id = ?", Long.class, userId);
        Long weekWorkouts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM treinos_realizados WHERE usuario_id = ? " +
                "AND YEARWEEK(realizado_em, 1) = YEARWEEK(CURDATE(), 1)", Long.class, userId);
        Long monthWorkouts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM treinos_realizados WHERE usuario_id = ? " +
                "AND YEAR(realizado_em) = YEAR(CURDATE()) AND MONTH(realizado_em) = MONTH(CURDATE())", Long.class, userId);
        Long totalExercises = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exercicios_realizados er " +
                "INNER JOIN treinos_realizados tr ON tr.id = er.treino_id WHERE tr.usuario_id = ?", Long.class, userId);
        BigDecimal totalVolume = jdbc.queryForObject(
                "SELECT COALESCE(SUM(volume_total), 0) FROM treinos_realizados WHERE usuario_id = ?",
                BigDecimal.class, userId);

        List<ProgressResponse.WeeklyVolume> weekly = jdbc.query(
                "SELECT YEARWEEK(realizado_em, 1) AS week_key, " +
                "DATE_FORMAT(MIN(realizado_em), '%d/%m') AS week_label, " +
                "COALESCE(SUM(volume_total),0) AS volume " +
                "FROM treinos_realizados WHERE usuario_id = ? " +
                "AND realizado_em >= DATE_SUB(CURDATE(), INTERVAL 41 DAY) " +
                "GROUP BY YEARWEEK(realizado_em, 1) ORDER BY week_key",
                (rs, rowNum) -> new ProgressResponse.WeeklyVolume(
                        rs.getString("week_label"), rs.getBigDecimal("volume").doubleValue()
                ), userId
        );

        List<ProgressResponse.PersonalRecord> records = jdbc.query(
                "SELECT er.exercicio_nome_snapshot AS exercise, MAX(sr.carga) AS weight, " +
                "MAX(CASE WHEN sr.carga = (SELECT MAX(sr2.carga) FROM series_realizadas sr2 " +
                "INNER JOIN exercicios_realizados er2 ON er2.id = sr2.exercicio_realizado_id " +
                "INNER JOIN treinos_realizados tr2 ON tr2.id = er2.treino_id " +
                "WHERE tr2.usuario_id = ? AND er2.exercicio_nome_snapshot = er.exercicio_nome_snapshot) " +
                "THEN sr.repeticoes ELSE 0 END) AS reps " +
                "FROM exercicios_realizados er " +
                "INNER JOIN treinos_realizados tr ON tr.id = er.treino_id " +
                "INNER JOIN series_realizadas sr ON sr.exercicio_realizado_id = er.id " +
                "WHERE tr.usuario_id = ? GROUP BY er.exercicio_nome_snapshot " +
                "ORDER BY weight DESC LIMIT 10",
                (rs, rowNum) -> new ProgressResponse.PersonalRecord(
                        rs.getString("exercise"),
                        rs.getBigDecimal("weight").doubleValue(),
                        rs.getInt("reps")
                ), userId, userId
        );

        List<java.time.LocalDate> workoutDates = jdbc.query(
                "SELECT DISTINCT DATE(realizado_em) AS workout_date FROM treinos_realizados " +
                "WHERE usuario_id = ? ORDER BY workout_date DESC",
                (rs, rowNum) -> rs.getDate("workout_date").toLocalDate(), userId
        );

        int streak = 0;
        java.time.LocalDate cursor = java.time.LocalDate.now();
        for (java.time.LocalDate date : workoutDates) {
            if (date.equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else if (date.isBefore(cursor)) {
                break;
            }
        }

        return new ProgressResponse(
                totalWorkouts == null ? 0 : totalWorkouts,
                weekWorkouts == null ? 0 : weekWorkouts,
                monthWorkouts == null ? 0 : monthWorkouts,
                totalExercises == null ? 0 : totalExercises,
                totalVolume == null ? 0 : totalVolume.doubleValue(),
                streak,
                weekly,
                records
        );
    }

    private int parseFirstInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        String digits = value.trim().split("[-–—]")[0].replaceAll("[^0-9]", "");
        if (digits.isBlank()) return fallback;
        try { return Integer.parseInt(digits); } catch (NumberFormatException e) { return fallback; }
    }

    public record UserRecord(Long id, String name, String email, String passwordHash) {}
    private record PlanRow(Long id, String name, Integer restDays, Boolean active,
                           Timestamp createdAt, Timestamp updatedAt) {}
    private record PlanDaySnapshot(Long id, String dayName, String planName) {}
    private record ExerciseSnapshot(Long id, String name, String sets, String reps, BigDecimal weight, int order) {}
}
