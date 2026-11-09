-- FERRO - Dados de teste para alimentar History e Progress
-- Execute somente depois de criar pelo menos um usuário e um plano com exercícios.
-- O script usa o primeiro usuário e o plano ativo encontrados no banco.
-- Pode ser executado no phpMyAdmin/MariaDB para preencher a aplicação com histórico.

USE ferro_db;

DROP PROCEDURE IF EXISTS seed_ferro_statistics;
DELIMITER $$

CREATE PROCEDURE seed_ferro_statistics()
BEGIN
    DECLARE v_user_id BIGINT UNSIGNED;
    DECLARE v_plan_id BIGINT UNSIGNED;
    DECLARE v_plan_name VARCHAR(120);
    DECLARE v_day_id BIGINT UNSIGNED;
    DECLARE v_day_name VARCHAR(120);
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_workout_id BIGINT UNSIGNED;

    SELECT id INTO v_user_id
      FROM usuarios
      ORDER BY id
      LIMIT 1;

    IF v_user_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Create a FERRO user before running this seed.';
    END IF;

    SELECT id, nome INTO v_plan_id, v_plan_name
      FROM planos
      WHERE usuario_id = v_user_id
      ORDER BY ativo DESC, id DESC
      LIMIT 1;

    IF v_plan_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Create a FERRO workout plan before running this seed.';
    END IF;

    SELECT id, nome INTO v_day_id, v_day_name
      FROM plano_dias
      WHERE plano_id = v_plan_id
      ORDER BY dia_semana, id
      LIMIT 1;

    IF v_day_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The selected plan has no workout days.';
    END IF;

    WHILE v_i < 12 DO
        INSERT INTO treinos_realizados
            (usuario_id, plano_id, plano_nome_snapshot, dia_nome_snapshot,
             realizado_em, exercicios_total, volume_total)
        VALUES
            (v_user_id, v_plan_id, v_plan_name, v_day_name,
             DATE_SUB(NOW(), INTERVAL (v_i * 3) DAY), 0, 0);

        SET v_workout_id = LAST_INSERT_ID();

        INSERT INTO exercicios_realizados
            (treino_id, exercicio_id, exercicio_nome_snapshot, ordem)
        SELECT
            v_workout_id, e.id, e.nome, e.ordem
        FROM exercicios e
        WHERE e.plano_dia_id = v_day_id;

        INSERT INTO series_realizadas
            (exercicio_realizado_id, numero_serie, repeticoes, carga)
        SELECT
            er.id,
            seq.numero_serie,
            COALESCE(CAST(SUBSTRING_INDEX(e.repeticoes, '-', 1) AS UNSIGNED), 0),
            COALESCE(e.carga, 0)
        FROM exercicios_realizados er
        INNER JOIN exercicios e ON e.id = er.exercicio_id
        INNER JOIN (
            SELECT 1 numero_serie UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL
            SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
        ) seq ON seq.numero_serie <= COALESCE(CAST(SUBSTRING_INDEX(e.series, '-', 1) AS UNSIGNED), 1)
        WHERE er.treino_id = v_workout_id;

        UPDATE treinos_realizados tr
        SET
            exercicios_total = (
                SELECT COUNT(*) FROM exercicios_realizados er WHERE er.treino_id = tr.id
            ),
            volume_total = (
                SELECT COALESCE(SUM(sr.repeticoes * sr.carga), 0)
                FROM exercicios_realizados er
                INNER JOIN series_realizadas sr ON sr.exercicio_realizado_id = er.id
                WHERE er.treino_id = tr.id
            )
        WHERE tr.id = v_workout_id;

        SET v_i = v_i + 1;
    END WHILE;
END$$

DELIMITER ;
CALL seed_ferro_statistics();
DROP PROCEDURE IF EXISTS seed_ferro_statistics;

-- Confira os dados criados:
-- SELECT * FROM vw_historico_treinos ORDER BY realizado_em DESC;
