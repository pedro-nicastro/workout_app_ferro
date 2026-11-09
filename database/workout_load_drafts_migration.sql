-- FERRO: salvamento automatico das cargas durante a execucao do treino.
-- Execute uma unica vez em bancos existentes que ja possuem a estrutura principal.
USE ferro_db;

CREATE TABLE IF NOT EXISTS treino_cargas_rascunho (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT UNSIGNED NOT NULL,
    plano_id BIGINT UNSIGNED NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL COMMENT '0=domingo ... 6=sábado',
    data_treino DATE NOT NULL,
    exercise_id BIGINT UNSIGNED NOT NULL,
    carga DECIMAL(8,2) NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_treino_carga_rascunho (usuario_id, plano_id, dia_semana, data_treino, exercise_id),
    KEY idx_treino_carga_usuario_data (usuario_id, data_treino),
    CONSTRAINT fk_tcr_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tcr_plano FOREIGN KEY (plano_id) REFERENCES planos(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tcr_exercicio FOREIGN KEY (exercise_id) REFERENCES exercicios(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
