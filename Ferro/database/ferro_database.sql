-- ============================================================
-- FERRO - Banco de dados
-- MySQL / MariaDB
-- Compatível com MySQL 8+ e MariaDB moderno
-- ============================================================

CREATE DATABASE IF NOT EXISTS ferro_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ferro_db;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    termos_aceitos TINYINT(1) NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS perfis_usuario (
    usuario_id BIGINT UNSIGNED NOT NULL,
    unidade ENUM('kg','lb') NOT NULL DEFAULT 'kg',
    peso_atual DECIMAL(6,2) NULL,
    meta_peso DECIMAL(6,2) NULL,
    notificacao_lembrete TINYINT(1) NOT NULL DEFAULT 1,
    notificacao_recorde TINYINT(1) NOT NULL DEFAULT 1,
    notificacao_resumo TINYINT(1) NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_perfil_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS planos (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT UNSIGNED NOT NULL,
    nome VARCHAR(120) NOT NULL,
    dias_descanso TINYINT UNSIGNED NOT NULL DEFAULT 0,
    ativo TINYINT(1) NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_planos_usuario (usuario_id),
    CONSTRAINT fk_planos_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plano_dias (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plano_id BIGINT UNSIGNED NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=segunda ... 6=domingo',
    nome VARCHAR(120) NOT NULL,
    ordem TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plano_dia_semana (plano_id, dia_semana),
    UNIQUE KEY uk_plano_dia_ordem (plano_id, ordem),
    CONSTRAINT fk_plano_dias_plano
        FOREIGN KEY (plano_id) REFERENCES planos(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exercicios (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plano_dia_id BIGINT UNSIGNED NOT NULL,
    nome VARCHAR(160) NOT NULL,
    series VARCHAR(20) NULL,
    repeticoes VARCHAR(30) NULL,
    carga DECIMAL(8,2) NULL,
    ordem TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercicio_ordem (plano_dia_id, ordem),
    KEY idx_exercicios_dia (plano_dia_id),
    CONSTRAINT fk_exercicios_dia
        FOREIGN KEY (plano_dia_id) REFERENCES plano_dias(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS treinos_realizados (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT UNSIGNED NOT NULL,
    plano_id BIGINT UNSIGNED NULL,
    plano_nome_snapshot VARCHAR(120) NOT NULL,
    dia_nome_snapshot VARCHAR(120) NOT NULL,
    realizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exercicios_total INT UNSIGNED NOT NULL DEFAULT 0,
    volume_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_treinos_usuario_data (usuario_id, realizado_em),
    KEY idx_treinos_plano (plano_id),
    CONSTRAINT fk_treinos_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_treinos_plano
        FOREIGN KEY (plano_id) REFERENCES planos(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exercicios_realizados (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    treino_id BIGINT UNSIGNED NOT NULL,
    exercicio_id BIGINT UNSIGNED NULL,
    exercicio_nome_snapshot VARCHAR(160) NOT NULL,
    ordem TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    KEY idx_exreal_treino (treino_id),
    CONSTRAINT fk_exreal_treino
        FOREIGN KEY (treino_id) REFERENCES treinos_realizados(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS series_realizadas (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    exercicio_realizado_id BIGINT UNSIGNED NOT NULL,
    numero_serie TINYINT UNSIGNED NOT NULL,
    repeticoes INT UNSIGNED NOT NULL DEFAULT 0,
    carga DECIMAL(8,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_serie_realizada (exercicio_realizado_id, numero_serie),
    CONSTRAINT fk_series_exercicio_realizado
        FOREIGN KEY (exercicio_realizado_id) REFERENCES exercicios_realizados(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE OR REPLACE VIEW vw_resumo_planos AS
SELECT
    p.id AS plano_id,
    p.usuario_id,
    p.nome AS plano_nome,
    p.dias_descanso,
    p.ativo,
    p.criado_em,
    p.atualizado_em,
    COUNT(DISTINCT pd.id) AS dias_treino,
    COUNT(e.id) AS exercicios_total
FROM planos p
LEFT JOIN plano_dias pd ON pd.plano_id = p.id
LEFT JOIN exercicios e ON e.plano_dia_id = pd.id
GROUP BY
    p.id, p.usuario_id, p.nome, p.dias_descanso,
    p.ativo, p.criado_em, p.atualizado_em;

CREATE OR REPLACE VIEW vw_historico_treinos AS
SELECT
    tr.id AS treino_id,
    tr.usuario_id,
    tr.plano_id,
    tr.plano_nome_snapshot,
    tr.dia_nome_snapshot,
    tr.realizado_em,
    tr.exercicios_total,
    tr.volume_total
FROM treinos_realizados tr;

-- ============================================================
-- Consultas úteis
-- ============================================================

-- SELECT * FROM usuarios;
-- SELECT * FROM planos;
-- SELECT * FROM vw_resumo_planos;
-- SELECT * FROM vw_historico_treinos;

-- ============================================================
-- BANCO EXISTENTE
-- ============================================================
-- Se o banco já existia antes desta versão, execute:
-- database/weekday_migration.sql
--
-- A coluna dias_descanso permanece por compatibilidade, mas o backend
-- passa a calculá-la automaticamente a partir dos dias selecionados.
