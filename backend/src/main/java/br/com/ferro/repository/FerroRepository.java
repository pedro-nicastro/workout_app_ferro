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
import java.util.List;
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

    public record UserRecord(Long id, String name, String email, String passwordHash) {}
    private record PlanRow(Long id, String name, Integer restDays, Boolean active,
                           Timestamp createdAt, Timestamp updatedAt) {}
}
