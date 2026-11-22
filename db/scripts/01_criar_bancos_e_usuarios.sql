-- ============================================================
-- GEEKS - criação dos bancos e dos usuários do MySQL
-- ============================================================
-- Rodar UMA vez, como root (ou outro administrador). Pode ser repetido:
-- recria nada, só garante que bancos, usuários, senhas e permissões existem.
--
-- As senhas NÃO ficam neste arquivo. Elas chegam por variáveis de sessão,
-- definidas antes do SOURCE (o script criar_bancos_e_usuarios.ps1 faz isso
-- lendo o db.properties):
--   SET @senha_migrator = '...';
--   SET @senha_app      = '...';
--   SOURCE db/scripts/01_criar_bancos_e_usuarios.sql;
--
-- Usuários:
--   geeks_migrator@localhost  DDL nos bancos geeks e geeks_test (Flyway, reset
--                             do banco de teste, backup e teste de restauração).
--   geeks_app@localhost       usado pelo sistema: SELECT/INSERT/UPDATE em tudo.
--                             DELETE só na tabela reserva (concedido pela
--                             migration V4, porque a tabela precisa existir).
-- A aplicação nunca usa root.
-- ============================================================

CREATE DATABASE IF NOT EXISTS geeks
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS geeks_test
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- CREATE USER / ALTER USER não aceitam variável na senha: usa PREPARE.
SET @sql = CONCAT('CREATE USER IF NOT EXISTS ''geeks_migrator''@''localhost'' IDENTIFIED BY ', QUOTE(@senha_migrator));
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
SET @sql = CONCAT('ALTER USER ''geeks_migrator''@''localhost'' IDENTIFIED BY ', QUOTE(@senha_migrator));
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = CONCAT('CREATE USER IF NOT EXISTS ''geeks_app''@''localhost'' IDENTIFIED BY ', QUOTE(@senha_app));
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
SET @sql = CONCAT('ALTER USER ''geeks_app''@''localhost'' IDENTIFIED BY ', QUOTE(@senha_app));
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @sql = NULL;

-- Migrator: tudo nos bancos do sistema (inclui CREATE/DROP do geeks_test).
-- GRANT OPTION permite que a migration V4 conceda o DELETE em reserva ao app.
GRANT ALL PRIVILEGES ON geeks.*              TO 'geeks_migrator'@'localhost' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON geeks_test.*         TO 'geeks_migrator'@'localhost' WITH GRANT OPTION;
-- Banco descartável usado só para testar a restauração do backup
GRANT ALL PRIVILEGES ON geeks_restore_test.* TO 'geeks_migrator'@'localhost';

-- App: sem DELETE e sem DDL
GRANT SELECT, INSERT, UPDATE ON geeks.*      TO 'geeks_app'@'localhost';
GRANT SELECT, INSERT, UPDATE ON geeks_test.* TO 'geeks_app'@'localhost';

FLUSH PRIVILEGES;

SELECT user, host FROM mysql.user WHERE user IN ('geeks_migrator', 'geeks_app');
SHOW GRANTS FOR 'geeks_migrator'@'localhost';
SHOW GRANTS FOR 'geeks_app'@'localhost';
