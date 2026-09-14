-- Rode este script no MySQL Workbench antes de iniciar o backend pela primeira vez.
-- (O Hibernate cria as TABELAS sozinho a partir das entidades, mas o SCHEMA/banco
-- precisa existir antes. A URL de conexao ja tem createDatabaseIfNotExist=true,
-- entao este CREATE DATABASE aqui e so uma garantia/redundancia.)

CREATE DATABASE IF NOT EXISTS aracabeach
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
