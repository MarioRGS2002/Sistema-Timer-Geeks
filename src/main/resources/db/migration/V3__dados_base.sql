-- ============================================================
-- GEEKS - V3: dados base
-- Precos INFORMADOS PELA EQUIPE, A CONFIRMAR COM A LOJA:
--   R$ 10,00 por hora em todos os tipos de equipamento;
--   R$ 180,00 de mensalidade do Clube Geeks.
-- Mudar preco depois = nova migration (V5, V6...). Sessoes antigas nao mudam
-- porque sessao.valor_hora_contratado e congelado na abertura da sessao.
-- ============================================================

INSERT INTO tipo_equipamento (nome, descricao, valor_hora, max_participantes) VALUES
  ('PC',              'Computador',          10.00, 1),
  ('PlayStation',     'Console PlayStation', 10.00, 4),
  ('Nintendo Switch', 'Console Nintendo',    10.00, 4),
  ('Xadrez',          'Mesa de xadrez',      10.00, 2);

INSERT INTO plano_clube (nome_plano, valor_mensalidade) VALUES
  ('Clube Geeks', 180.00);

INSERT INTO forma_pagamento (nome, descricao) VALUES
  ('Dinheiro', NULL),
  ('PIX',      NULL),
  ('Débito',   NULL),
  ('Crédito',  NULL);
