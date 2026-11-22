-- ============================================================
-- GEEKS - V4: DELETE da tabela reserva para o usuario da aplicacao
-- O geeks_app tem SELECT/INSERT/UPDATE no banco todo (script
-- db/scripts/01_criar_bancos_e_usuarios.sql). DELETE e permitido SO em
-- reserva. O GRANT fica aqui (e nao no script de usuarios) porque o MySQL
-- so concede privilegio de tabela quando a tabela ja existe.
-- Executado pelo geeks_migrator, que tem GRANT OPTION no banco.
-- ============================================================

GRANT DELETE ON reserva TO 'geeks_app'@'localhost';
