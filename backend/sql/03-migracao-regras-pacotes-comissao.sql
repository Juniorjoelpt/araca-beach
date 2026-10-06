-- Migracao: regras de reserva, pacotes de aulas e comissao automatica.
--
-- Em ambientes com spring.jpa.hibernate.ddl-auto=update (padrao do projeto) as
-- TABELAS e COLUNAS novas sao criadas sozinhas ao subir o backend. O que o
-- Hibernate NAO faz e ampliar colunas que ja existem como ENUM do MySQL. Rode
-- este script UMA VEZ (antes ou logo depois do deploy) para aceitar os novos
-- valores OrigemReserva.AULA e CategoriaDespesa.COMISSAO. VARCHAR(20) funciona
-- tanto se a coluna hoje e ENUM quanto se ja e VARCHAR.
--
-- Banco: aracabeach (MySQL). Faca backup antes.

ALTER TABLE reservas MODIFY COLUMN origem VARCHAR(20) NOT NULL;
ALTER TABLE despesas MODIFY COLUMN categoria VARCHAR(20) NOT NULL;
ALTER TABLE despesas_recorrentes MODIFY COLUMN categoria VARCHAR(20) NOT NULL;
