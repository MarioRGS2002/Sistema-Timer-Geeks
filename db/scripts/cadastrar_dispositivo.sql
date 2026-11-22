-- ============================================================
-- GEEKS - cadastrar equipamentos em PRODUÇÃO (não existe tela para isso)
-- ============================================================
-- Rodar com o usuário geeks_migrator (ou geeks_app) no banco geeks:
--   mysql -u geeks_migrator -p geeks < db\scripts\cadastrar_dispositivo.sql
-- Edite a lista abaixo antes de rodar. O identificador é único (aparece nas telas).
-- Tipos existentes: 'PC', 'PlayStation', 'Nintendo Switch', 'Xadrez'.
-- Para tirar um equipamento de uso, NÃO apague: UPDATE equipamento SET status = 'INDISPONIVEL' ...
-- Reinicie o sistema depois de cadastrar (a lista de equipamentos é lida ao abrir).
-- ============================================================

INSERT INTO equipamento (identificador, id_tipo, data_aquisicao, status)
SELECT 'PC01', t.id_tipo, CURDATE(), 'DISPONIVEL'
  FROM tipo_equipamento AS t
 WHERE t.nome = 'PC'
   AND NOT EXISTS (SELECT 1 FROM equipamento AS e WHERE e.identificador = 'PC01');

-- Repita o bloco acima para cada equipamento, trocando o identificador e o tipo. Exemplos:
-- INSERT INTO equipamento (identificador, id_tipo, data_aquisicao, status)
-- SELECT 'Ps5', t.id_tipo, CURDATE(), 'DISPONIVEL' FROM tipo_equipamento AS t
--  WHERE t.nome = 'PlayStation' AND NOT EXISTS (SELECT 1 FROM equipamento AS e WHERE e.identificador = 'Ps5');

SELECT e.id_equipamento, e.identificador, t.nome AS tipo, e.status
  FROM equipamento AS e
 INNER JOIN tipo_equipamento AS t ON t.id_tipo = e.id_tipo
 ORDER BY e.identificador;
