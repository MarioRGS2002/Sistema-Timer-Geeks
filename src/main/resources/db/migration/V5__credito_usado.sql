-- ============================================================
-- GEEKS - V5: crédito de tempo usado em cada sessão
-- ============================================================
-- Quando o cliente usa o crédito (tempo pago antes e não usado) para abater o
-- tempo de uma sessão, o quanto foi usado fica registrado no participante
-- (como já acontece com a hora grátis do Clube). O saldo continua sendo
-- credito_tempo.minutos_restantes, abatido do crédito que vence primeiro.
-- NUNCA editar esta migration depois de aplicada.
-- ============================================================

ALTER TABLE participante_sessao
  ADD COLUMN segundos_credito INT UNSIGNED NOT NULL DEFAULT 0 AFTER segundos_gratis;
