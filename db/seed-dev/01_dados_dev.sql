-- ============================================================
-- GEEKS - dados de DESENVOLVIMENTO (nunca rodar em produção)
-- ============================================================
-- Carregar num banco recém-migrado e vazio:
--   java -cp target\sistema-geeks.jar geeks.tools.CarregarSeedDev
-- (a ferramenta recusa se já houver clientes cadastrados).
-- Datas relativas a NOW()/CURDATE(); idades calculadas a partir de hoje.
-- Valores coerentes com R$ 10,00/hora e com as regras de cobrança da aplicação
-- (classe geeks.model.Cobranca).
-- Senhas de dev (documentadas só no README): atendente / geeks123, gerente / gerente123.
-- ============================================================

-- Equipamentos
INSERT INTO equipamento (identificador, id_tipo, status)
SELECT 'PC01', id_tipo, 'DISPONIVEL' FROM tipo_equipamento WHERE nome = 'PC';
INSERT INTO equipamento (identificador, id_tipo, status)
SELECT 'PC02', id_tipo, 'DISPONIVEL' FROM tipo_equipamento WHERE nome = 'PC';
INSERT INTO equipamento (identificador, id_tipo, status)
SELECT 'Ps5', id_tipo, 'DISPONIVEL' FROM tipo_equipamento WHERE nome = 'PlayStation';
INSERT INTO equipamento (identificador, id_tipo, status)
SELECT 'Nintendo Switch', id_tipo, 'DISPONIVEL' FROM tipo_equipamento WHERE nome = 'Nintendo Switch';
INSERT INTO equipamento (identificador, id_tipo, status)
SELECT 'Xadrez', id_tipo, 'DISPONIVEL' FROM tipo_equipamento WHERE nome = 'Xadrez';

SET @pc01 = (SELECT id_equipamento FROM equipamento WHERE identificador = 'PC01');
SET @pc02 = (SELECT id_equipamento FROM equipamento WHERE identificador = 'PC02');
SET @ps5  = (SELECT id_equipamento FROM equipamento WHERE identificador = 'Ps5');
SET @nsw  = (SELECT id_equipamento FROM equipamento WHERE identificador = 'Nintendo Switch');
SET @xad  = (SELECT id_equipamento FROM equipamento WHERE identificador = 'Xadrez');
SET @tipo_pc  = (SELECT id_tipo FROM tipo_equipamento WHERE nome = 'PC');
SET @tipo_ps  = (SELECT id_tipo FROM tipo_equipamento WHERE nome = 'PlayStation');
SET @tipo_xad = (SELECT id_tipo FROM tipo_equipamento WHERE nome = 'Xadrez');

-- Funcionários (hash BCrypt custo 11 gerado com jbcrypt)
INSERT INTO funcionario (nome, login, senha_hash, cargo, data_admissao, turno_trabalho) VALUES
  ('Atendente', 'atendente', '$2a$11$kyOy7EaeG0XL7Cp0HZS9JugkGdmSqQ.C/Hj8zL70ueyfw2G56Elxe', 'ATENDENTE', CURDATE() - INTERVAL 1 YEAR, 'TARDE'),
  ('Gerente',   'gerente',   '$2a$11$qkd6fcdKmBaaCDvaq1MawOrdwAsq8to7ds.cY4HUX31UowCqTz0UG', 'GERENTE',   CURDATE() - INTERVAL 2 YEAR, 'INTEGRAL');
SET @atd = (SELECT id_funcionario FROM funcionario WHERE login = 'atendente');

-- Clientes (idade pedida a partir de hoje)
INSERT INTO cliente (nome, cpf, data_nascimento, telefone) VALUES
  ('Gabriel Dantas',  '11144477735', CURDATE() - INTERVAL 22 YEAR - INTERVAL 40 DAY,  '(75) 99999-0001'),
  ('Mario Romero',    '52998224725', CURDATE() - INTERVAL 25 YEAR - INTERVAL 100 DAY, '(75) 99999-0002'),
  ('Wbyrathan',       '39053344705', CURDATE() - INTERVAL 19 YEAR - INTERVAL 10 DAY,  '(75) 99999-0003'),
  ('Matheus Barbosa', '16899535009', CURDATE() - INTERVAL 20 YEAR - INTERVAL 200 DAY, '(75) 99999-0004'),
  ('Nicolas Aragão',  '11122233396', CURDATE() - INTERVAL 16 YEAR - INTERVAL 60 DAY,  '(75) 99999-0005');
SET @gabriel = (SELECT id_cliente FROM cliente WHERE cpf = '11144477735');
SET @mario   = (SELECT id_cliente FROM cliente WHERE cpf = '52998224725');
SET @wby     = (SELECT id_cliente FROM cliente WHERE cpf = '39053344705');
SET @matheus = (SELECT id_cliente FROM cliente WHERE cpf = '16899535009');
SET @nicolas = (SELECT id_cliente FROM cliente WHERE cpf = '11122233396');

-- Responsável do menor (CPF fictício válido)
INSERT INTO responsavel (id_cliente, nome, cpf, telefone, parentesco) VALUES
  (@nicolas, 'Fernanda Aragão', '12345678909', '(75) 99999-0006', 'Mãe');

-- Clube: Gabriel em dia; Matheus com assinatura ATIVA porém vencida (data_fim no passado)
SET @plano = (SELECT id_plano FROM plano_clube WHERE nome_plano = 'Clube Geeks');
SET @pix = (SELECT id_forma_pagamento FROM forma_pagamento WHERE nome = 'PIX');
SET @dinheiro = (SELECT id_forma_pagamento FROM forma_pagamento WHERE nome = 'Dinheiro');
SET @debito = (SELECT id_forma_pagamento FROM forma_pagamento WHERE nome = 'Débito');

INSERT INTO assinatura_clube (data_inicio, data_fim, situacao, id_plano, id_cliente) VALUES
  (CURDATE() - INTERVAL 10 DAY, CURDATE() - INTERVAL 10 DAY + INTERVAL 1 MONTH - INTERVAL 1 DAY, 'ATIVA', @plano, @gabriel);
SET @ass_gabriel = LAST_INSERT_ID();
INSERT INTO assinatura_clube (data_inicio, data_fim, situacao, id_plano, id_cliente) VALUES
  (CURDATE() - INTERVAL 4 DAY - INTERVAL 1 MONTH, CURDATE() - INTERVAL 5 DAY, 'ATIVA', @plano, @matheus);
SET @ass_matheus = LAST_INSERT_ID();
INSERT INTO pagamento (data_hora_pagamento, valor_pago, tipo_pagamento, id_forma_pagamento, id_funcionario, id_assinatura) VALUES
  (TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '10:00:00'), 180.00, 'ASSINATURA', @pix, @atd, @ass_gabriel),
  (TIMESTAMP(CURDATE() - INTERVAL 4 DAY - INTERVAL 1 MONTH, '11:00:00'), 180.00, 'ASSINATURA', @dinheiro, @atd, @ass_matheus);

-- ------------------------------------------------------------
-- Histórico encerrado (com pagamentos coerentes)
-- ------------------------------------------------------------

-- H1 sem Clube: Mario, PC02, FECHADO 2 h, R$ 20,00 em dinheiro
SET @ini = TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '18:00:00');
INSERT INTO sessao (horario_inicio, horario_previsto, horario_fim, tipo_tempo, status_sessao, valor_hora_contratado, valor_total_devido, id_equipamento, id_funcionario)
VALUES (@ini, @ini + INTERVAL 120 MINUTE, @ini + INTERVAL 120 MINUTE, 'FECHADO', 'ENCERRADA', 10.00, 20.00, @pc02, @atd);
SET @s = LAST_INSERT_ID();
INSERT INTO participante_sessao (id_sessao, id_cliente, segundos_gratis) VALUES (@s, @mario, 0);
INSERT INTO pagamento (data_hora_pagamento, valor_pago, momento_pagamento, tipo_pagamento, id_forma_pagamento, id_funcionario, id_sessao)
VALUES (@ini + INTERVAL 120 MINUTE, 20.00, 'SAIDA', 'SESSAO', @dinheiro, @atd, @s);

-- H2 Clube com hora grátis inteira: Gabriel, PC01, FECHADO 90 min.
-- 3600 s grátis (pagamento 0, sem forma) + 30 min pagos = R$ 5,00 no PIX
SET @ini = TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '14:00:00');
INSERT INTO sessao (horario_inicio, horario_previsto, horario_fim, tipo_tempo, status_sessao, valor_hora_contratado, valor_total_devido, id_equipamento, id_funcionario)
VALUES (@ini, @ini + INTERVAL 90 MINUTE, @ini + INTERVAL 90 MINUTE, 'FECHADO', 'ENCERRADA', 10.00, 5.00, @pc01, @atd);
SET @s = LAST_INSERT_ID();
INSERT INTO participante_sessao (id_sessao, id_cliente, segundos_gratis) VALUES (@s, @gabriel, 3600);
INSERT INTO pagamento (data_hora_pagamento, valor_pago, momento_pagamento, tipo_pagamento, id_forma_pagamento, id_funcionario, id_sessao) VALUES
  (@ini + INTERVAL 90 MINUTE, 0.00, 'SAIDA', 'SESSAO', NULL, @atd, @s),
  (@ini + INTERVAL 90 MINUTE, 5.00, 'SAIDA', 'SESSAO', @pix,  @atd, @s);

-- H3 hora grátis parcial no mesmo dia (ontem), Gabriel:
--  a) 10:00-10:40, 2400 s grátis, sessão TOTALMENTE GRÁTIS (pagamento 0, sem forma)
SET @ini = TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '10:00:00');
INSERT INTO sessao (horario_inicio, horario_previsto, horario_fim, tipo_tempo, status_sessao, valor_hora_contratado, valor_total_devido, id_equipamento, id_funcionario)
VALUES (@ini, @ini + INTERVAL 40 MINUTE, @ini + INTERVAL 40 MINUTE, 'FECHADO', 'ENCERRADA', 10.00, 0.00, @pc01, @atd);
SET @s = LAST_INSERT_ID();
INSERT INTO participante_sessao (id_sessao, id_cliente, segundos_gratis) VALUES (@s, @gabriel, 2400);
INSERT INTO pagamento (data_hora_pagamento, valor_pago, momento_pagamento, tipo_pagamento, id_forma_pagamento, id_funcionario, id_sessao)
VALUES (@ini + INTERVAL 40 MINUTE, 0.00, 'SAIDA', 'SESSAO', NULL, @atd, @s);
--  b) 15:00-16:00, só restam 1200 s grátis; 40 min pagos = R$ 6,67 no débito (soma do dia: 3600 s)
SET @ini = TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '15:00:00');
INSERT INTO sessao (horario_inicio, horario_previsto, horario_fim, tipo_tempo, status_sessao, valor_hora_contratado, valor_total_devido, id_equipamento, id_funcionario)
VALUES (@ini, @ini + INTERVAL 60 MINUTE, @ini + INTERVAL 60 MINUTE, 'FECHADO', 'ENCERRADA', 10.00, 6.67, @ps5, @atd);
SET @s = LAST_INSERT_ID();
INSERT INTO participante_sessao (id_sessao, id_cliente, segundos_gratis) VALUES (@s, @gabriel, 1200);
INSERT INTO pagamento (data_hora_pagamento, valor_pago, momento_pagamento, tipo_pagamento, id_forma_pagamento, id_funcionario, id_sessao) VALUES
  (@ini + INTERVAL 60 MINUTE, 0.00, 'SAIDA', 'SESSAO', NULL,    @atd, @s),
  (@ini + INTERVAL 60 MINUTE, 6.67, 'SAIDA', 'SESSAO', @debito, @atd, @s);

-- H4 ABERTA com 30 min de uso: Matheus (assinatura vencida, sem grátis), Ps5.
-- Paga o mínimo de 1 h (R$ 10,00) e gera 30 min de crédito válido por 3 meses.
SET @ini = TIMESTAMP(CURDATE() - INTERVAL 4 DAY, '19:00:00');
INSERT INTO sessao (horario_inicio, horario_fim, tipo_tempo, status_sessao, valor_hora_contratado, valor_total_devido, id_equipamento, id_funcionario)
VALUES (@ini, @ini + INTERVAL 30 MINUTE, 'ABERTO', 'ENCERRADA', 10.00, 10.00, @ps5, @atd);
SET @s = LAST_INSERT_ID();
INSERT INTO participante_sessao (id_sessao, id_cliente, segundos_gratis) VALUES (@s, @matheus, 0);
INSERT INTO pagamento (data_hora_pagamento, valor_pago, momento_pagamento, tipo_pagamento, id_forma_pagamento, id_funcionario, id_sessao)
VALUES (@ini + INTERVAL 30 MINUTE, 10.00, 'SAIDA', 'SESSAO', @pix, @atd, @s);
INSERT INTO credito_tempo (id_cliente, id_tipo, id_sessao_origem, minutos_restantes, data_geracao, valido_ate)
VALUES (@matheus, @tipo_ps, @s, 30, @ini + INTERVAL 30 MINUTE, DATE(@ini + INTERVAL 30 MINUTE) + INTERVAL 3 MONTH);

-- H5 crédito JÁ VENCIDO: Wbyrathan, Xadrez, ABERTO 20 min há 4 meses (40 min de crédito, validade passou)
SET @ini = TIMESTAMP(CURDATE() - INTERVAL 4 MONTH, '16:00:00');
INSERT INTO sessao (horario_inicio, horario_fim, tipo_tempo, status_sessao, valor_hora_contratado, valor_total_devido, id_equipamento, id_funcionario)
VALUES (@ini, @ini + INTERVAL 20 MINUTE, 'ABERTO', 'ENCERRADA', 10.00, 10.00, @xad, @atd);
SET @s = LAST_INSERT_ID();
INSERT INTO participante_sessao (id_sessao, id_cliente, segundos_gratis) VALUES (@s, @wby, 0);
INSERT INTO pagamento (data_hora_pagamento, valor_pago, momento_pagamento, tipo_pagamento, id_forma_pagamento, id_funcionario, id_sessao)
VALUES (@ini + INTERVAL 20 MINUTE, 10.00, 'SAIDA', 'SESSAO', @dinheiro, @atd, @s);
INSERT INTO credito_tempo (id_cliente, id_tipo, id_sessao_origem, minutos_restantes, data_geracao, valido_ate)
VALUES (@wby, @tipo_xad, @s, 40, @ini + INTERVAL 20 MINUTE, DATE(@ini + INTERVAL 20 MINUTE) + INTERVAL 3 MONTH);

-- H6 Nicolas (menor, com responsável), Switch, FECHADO 1 h, R$ 10,00
SET @ini = TIMESTAMP(CURDATE() - INTERVAL 6 DAY, '17:00:00');
INSERT INTO sessao (horario_inicio, horario_previsto, horario_fim, tipo_tempo, status_sessao, valor_hora_contratado, valor_total_devido, id_equipamento, id_funcionario)
VALUES (@ini, @ini + INTERVAL 60 MINUTE, @ini + INTERVAL 60 MINUTE, 'FECHADO', 'ENCERRADA', 10.00, 10.00, @nsw, @atd);
SET @s = LAST_INSERT_ID();
INSERT INTO participante_sessao (id_sessao, id_cliente, segundos_gratis) VALUES (@s, @nicolas, 0);
INSERT INTO pagamento (data_hora_pagamento, valor_pago, momento_pagamento, tipo_pagamento, id_forma_pagamento, id_funcionario, id_sessao)
VALUES (@ini + INTERVAL 60 MINUTE, 10.00, 'SAIDA', 'SESSAO', @dinheiro, @atd, @s);

-- ------------------------------------------------------------
-- 5 sessões ativas (início há 92, 48, 130, 65 e 25 min)
-- ------------------------------------------------------------
SET @agora = NOW();
INSERT INTO sessao (horario_inicio, horario_previsto, tipo_tempo, valor_hora_contratado, id_equipamento, id_funcionario)
VALUES (@agora - INTERVAL 92 MINUTE, @agora - INTERVAL 92 MINUTE + INTERVAL 180 MINUTE, 'FECHADO', 10.00, @pc01, @atd);
INSERT INTO participante_sessao (id_sessao, id_cliente) VALUES (LAST_INSERT_ID(), @gabriel);

INSERT INTO sessao (horario_inicio, tipo_tempo, valor_hora_contratado, id_equipamento, id_funcionario)
VALUES (@agora - INTERVAL 48 MINUTE, 'ABERTO', 10.00, @pc02, @atd);
INSERT INTO participante_sessao (id_sessao, id_cliente) VALUES (LAST_INSERT_ID(), @mario);

INSERT INTO sessao (horario_inicio, horario_previsto, tipo_tempo, valor_hora_contratado, id_equipamento, id_funcionario)
VALUES (@agora - INTERVAL 130 MINUTE, @agora - INTERVAL 130 MINUTE + INTERVAL 180 MINUTE, 'FECHADO', 10.00, @xad, @atd);
INSERT INTO participante_sessao (id_sessao, id_cliente) VALUES (LAST_INSERT_ID(), @wby);

INSERT INTO sessao (horario_inicio, tipo_tempo, valor_hora_contratado, id_equipamento, id_funcionario)
VALUES (@agora - INTERVAL 65 MINUTE, 'ABERTO', 10.00, @ps5, @atd);
INSERT INTO participante_sessao (id_sessao, id_cliente) VALUES (LAST_INSERT_ID(), @matheus);

INSERT INTO sessao (horario_inicio, horario_previsto, tipo_tempo, valor_hora_contratado, id_equipamento, id_funcionario)
VALUES (@agora - INTERVAL 25 MINUTE, @agora - INTERVAL 25 MINUTE + INTERVAL 60 MINUTE, 'FECHADO', 10.00, @nsw, @atd);
INSERT INTO participante_sessao (id_sessao, id_cliente) VALUES (LAST_INSERT_ID(), @nicolas);

UPDATE equipamento SET status = 'EM_USO' WHERE id_equipamento IN (@pc01, @pc02, @ps5, @nsw, @xad);

-- ------------------------------------------------------------
-- Reservas futuras (sempre tempo FECHADO)
-- ------------------------------------------------------------
INSERT INTO reserva (inicio_previsto, fim_previsto, status_reserva, valor_previsto, percentual_entrada_minima, valor_entrada_minima, id_cliente, id_equipamento)
VALUES (TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '15:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '16:00:00'),
        'PENDENTE_ENTRADA', 10.00, 50.00, 5.00, @gabriel, @pc01);
INSERT INTO reserva (inicio_previsto, fim_previsto, status_reserva, valor_previsto, percentual_entrada_minima, valor_entrada_minima, id_cliente, id_equipamento)
VALUES (TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '10:00:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '11:00:00'),
        'CONFIRMADA', 10.00, 50.00, 5.00, @mario, @pc02);
INSERT INTO pagamento (data_hora_pagamento, valor_pago, momento_pagamento, tipo_pagamento, id_forma_pagamento, id_funcionario, id_reserva)
VALUES (NOW() - INTERVAL 3 HOUR, 5.00, 'ENTRADA', 'RESERVA', @pix, @atd, LAST_INSERT_ID());
