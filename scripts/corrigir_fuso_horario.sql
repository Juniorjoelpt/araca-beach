-- Corrige o horario de registros antigos: o sistema gravava em UTC (3h a frente de Brasilia/Fortaleza).
-- Subtrai 3 horas dos carimbos de data/hora gerados pelo sistema (criacao, abertura, fechamento, pagamentos).
-- NAO mexe em horarios digitados por pessoas (inicio/fim de reservas, hora marcada de mesa).
--
-- COMO RODAR (uma unica vez, com o backend PARADO, para nenhum registro novo ser criado no meio):
--   cd ~/araca-beach && git pull origin main && docker compose build
--   docker compose stop backend
--   docker compose exec -T mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" aracabeach' < scripts/corrigir_fuso_horario.sql
--   docker compose up -d
-- O script tem trava: se rodar de novo, nao faz nada (nao desloca 2 vezes).

CREATE TABLE IF NOT EXISTS ajustes_aplicados (
  nome VARCHAR(80) NOT NULL PRIMARY KEY,
  aplicado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

SET @pendente = (SELECT COUNT(*) = 0 FROM ajustes_aplicados WHERE nome = 'fuso-utc-para-fortaleza');

UPDATE pagamentos SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE rest_comandas SET aberta_em = aberta_em - INTERVAL 3 HOUR WHERE @pendente AND aberta_em IS NOT NULL;
UPDATE rest_comandas SET fechada_em = fechada_em - INTERVAL 3 HOUR WHERE @pendente AND fechada_em IS NOT NULL;
UPDATE rest_pedidos SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE rest_pagamentos SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE rest_movimentos_insumo SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE rest_reservas_mesa SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE comandas SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE comandas SET fechada_em = fechada_em - INTERVAL 3 HOUR WHERE @pendente AND fechada_em IS NOT NULL;
UPDATE movimentacoes_estoque SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE mensalidades SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE reservas SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE reservas SET cancelada_em = cancelada_em - INTERVAL 3 HOUR WHERE @pendente AND cancelada_em IS NOT NULL;
UPDATE clientes SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;
UPDATE auditoria SET criado_em = criado_em - INTERVAL 3 HOUR WHERE @pendente AND criado_em IS NOT NULL;

INSERT INTO ajustes_aplicados (nome) SELECT 'fuso-utc-para-fortaleza' WHERE @pendente;

SELECT IF(@pendente, 'Fuso corrigido: -3h aplicado.', 'Ja tinha sido aplicado antes: nada foi alterado.') AS resultado;
