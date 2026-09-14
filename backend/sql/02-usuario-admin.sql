-- Rode este script DEPOIS de iniciar o backend pelo menos uma vez
-- (para que a tabela "usuarios" ja exista, criada pelo Hibernate).
-- Login: admin | Senha: admin123

USE aracabeach;

INSERT INTO usuarios (nome, login, senha_hash, perfil, ativo)
VALUES ('Administrador', 'admin', '$2b$10$b5MpzlTLe9esmCwl1fECweAW5P.B9vJGKRpDilVttfjg1qBex6JMO', 'ADMIN', true);
