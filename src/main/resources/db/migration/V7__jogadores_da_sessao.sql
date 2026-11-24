-- ============================================================
-- GEEKS - V7: vários jogadores na mesma sessão
-- ============================================================
-- A sessão continua tendo um TITULAR (quem abriu e paga, com a hora grátis
-- do Clube e o crédito dele). Os outros jogadores (amigos no mesmo console)
-- entram como participantes com titular = 0: ficam registrados no histórico
-- e não podem estar em outra sessão ao mesmo tempo. O limite de jogadores é
-- tipo_equipamento.max_participantes (regra da aplicação).
-- Linhas que já existem são todas de titulares (DEFAULT 1).
-- NUNCA editar esta migration depois de aplicada.
-- ============================================================

ALTER TABLE participante_sessao
  ADD COLUMN titular TINYINT(1) NOT NULL DEFAULT 1 AFTER id_cliente;
