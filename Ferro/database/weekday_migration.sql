-- ============================================================
-- FERRO - Migração para dias fixos da semana
-- Execute este arquivo UMA VEZ em um banco que já possui a tabela
-- plano_dias criada pela versão anterior do projeto.
-- ============================================================

USE ferro_db;

ALTER TABLE plano_dias
    ADD COLUMN dia_semana TINYINT UNSIGNED NOT NULL DEFAULT 0
    AFTER plano_id;

-- Na versão antiga, ordem 0..6 representava apenas a posição no ciclo.
-- Aqui ela é usada como melhor aproximação inicial:
-- 0=domingo, 1=segunda, ..., 6=sábado.
-- Como a versão antiga começava na segunda, deslocamos a posição em +1.
UPDATE plano_dias
SET dia_semana = MOD(ordem + 1, 7);

ALTER TABLE plano_dias
    ADD UNIQUE KEY uk_plano_dia_semana (plano_id, dia_semana);

-- Verificação:
-- SELECT id, plano_id, dia_semana, nome, ordem
-- FROM plano_dias
-- ORDER BY plano_id, dia_semana;
