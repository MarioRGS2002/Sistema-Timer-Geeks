-- ============================================================
-- GEEKS - V2: tabelas de movimento
-- assinatura_clube, sessao, reserva, participante_sessao, pagamento,
-- credito_tempo e a view vw_menor_sem_responsavel
-- NUNCA editar esta migration depois de aplicada: mudanças vão em V5, V6...
-- ============================================================

-- 7. ASSINATURA DO CLUBE
-- cliente_ativo e coluna gerada: so permite 1 assinatura ATIVA por cliente.
CREATE TABLE assinatura_clube (
  id_assinatura INT NOT NULL AUTO_INCREMENT,
  data_inicio   DATE NOT NULL,
  data_fim      DATE NOT NULL,
  situacao      VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
  id_plano      INT NOT NULL,
  id_cliente    INT NOT NULL,
  cliente_ativo INT GENERATED ALWAYS AS
    (IF(situacao = 'ATIVA', id_cliente, NULL)) STORED,
  PRIMARY KEY (id_assinatura),
  CONSTRAINT uq_assinatura_ativa UNIQUE (cliente_ativo),
  CONSTRAINT fk_assinatura_plano
    FOREIGN KEY (id_plano) REFERENCES plano_clube(id_plano),
  CONSTRAINT fk_assinatura_cliente
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  CONSTRAINT ck_assinatura_situacao
    CHECK (situacao IN ('ATIVA','VENCIDA','CANCELADA')),
  CONSTRAINT ck_assinatura_datas
    CHECK (data_fim >= data_inicio)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 9. SESSAO
-- Uma sessao pode ocorrer sem reserva (a ligacao fica em reserva.id_sessao).
-- equipamento_em_uso e coluna gerada: so permite 1 sessao aberta por equipamento.
-- tipo_tempo: FECHADO tem horario_previsto; ABERTO nao tem (o cliente decide
-- quando acabar). limite_reserva (so no ABERTO): hora combinada com o cliente
-- para liberar o equipamento por causa de uma reserva. Cobranca minima de 1 hora
-- no ABERTO e geracao do credito com o tempo pago e nao usado: regras de calculo
-- da aplicacao.
CREATE TABLE sessao (
  id_sessao             INT NOT NULL AUTO_INCREMENT,
  horario_inicio        DATETIME NOT NULL,
  horario_previsto      DATETIME,
  limite_reserva        DATETIME,
  horario_fim           DATETIME,
  tipo_tempo            VARCHAR(10) NOT NULL,
  status_sessao         VARCHAR(20) NOT NULL DEFAULT 'EM_ANDAMENTO',
  notificacao_enviada   TINYINT(1) NOT NULL DEFAULT 0,
  valor_hora_contratado DECIMAL(10,2) NOT NULL,
  valor_total_devido    DECIMAL(10,2),
  id_equipamento        INT NOT NULL,
  id_funcionario        INT NOT NULL,
  equipamento_em_uso    INT GENERATED ALWAYS AS
    (IF(horario_fim IS NULL, id_equipamento, NULL)) STORED,
  PRIMARY KEY (id_sessao),
  CONSTRAINT uq_sessao_equipamento_em_uso UNIQUE (equipamento_em_uso),
  INDEX idx_sessao_fim (horario_fim),
  INDEX idx_sessao_inicio (horario_inicio),
  CONSTRAINT fk_sessao_equipamento
    FOREIGN KEY (id_equipamento) REFERENCES equipamento(id_equipamento),
  CONSTRAINT fk_sessao_funcionario
    FOREIGN KEY (id_funcionario) REFERENCES funcionario(id_funcionario),
  CONSTRAINT ck_sessao_previsto
    CHECK (horario_previsto IS NULL OR horario_previsto > horario_inicio),
  CONSTRAINT ck_sessao_limite
    CHECK (limite_reserva IS NULL
           OR (tipo_tempo = 'ABERTO' AND limite_reserva > horario_inicio)),
  CONSTRAINT ck_sessao_tipo_tempo CHECK (tipo_tempo IN ('ABERTO','FECHADO')),
  CONSTRAINT ck_sessao_tipo_previsto
    CHECK ((tipo_tempo = 'FECHADO') = (horario_previsto IS NOT NULL)),
  CONSTRAINT ck_sessao_fim CHECK (horario_fim IS NULL OR horario_fim >= horario_inicio),
  CONSTRAINT ck_sessao_status CHECK (status_sessao IN ('EM_ANDAMENTO','ENCERRADA')),
  CONSTRAINT ck_sessao_status_fim
    CHECK ((horario_fim IS NULL) = (status_sessao = 'EM_ANDAMENTO')),
  CONSTRAINT ck_sessao_valor_hora CHECK (valor_hora_contratado >= 0),
  CONSTRAINT ck_sessao_valor_total CHECK (valor_total_devido IS NULL OR valor_total_devido >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 10. RESERVA
-- Sempre em tempo FECHADO (inicio e fim previstos). Tempo ABERTO so e aberto
-- presencialmente, direto na sessao.
-- Pode ser cadastrada antes de existir a sessao (id_sessao NULL).
-- Recebe id_sessao como chave estrangeira quando a reserva vira sessao.
-- O valor da entrada e armazenado no campo valor_entrada_minima.
-- Reserva nao e apagada: cancelar = status_reserva 'CANCELADA'.
CREATE TABLE reserva (
  id_reserva                INT NOT NULL AUTO_INCREMENT,
  inicio_previsto           DATETIME NOT NULL,
  fim_previsto              DATETIME NOT NULL,
  status_reserva            VARCHAR(20) NOT NULL DEFAULT 'PENDENTE_ENTRADA',
  valor_previsto            DECIMAL(10,2) NOT NULL,
  percentual_entrada_minima DECIMAL(5,2) NOT NULL DEFAULT 50.00,
  valor_entrada_minima      DECIMAL(10,2),
  id_cliente                INT NOT NULL,
  id_equipamento            INT NOT NULL,
  id_sessao                 INT,
  PRIMARY KEY (id_reserva),
  UNIQUE (id_sessao),
  INDEX idx_reserva_equipamento_inicio (id_equipamento, inicio_previsto),
  INDEX idx_reserva_inicio (inicio_previsto),
  CONSTRAINT fk_reserva_cliente
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  CONSTRAINT fk_reserva_equipamento
    FOREIGN KEY (id_equipamento) REFERENCES equipamento(id_equipamento),
  CONSTRAINT fk_reserva_sessao
    FOREIGN KEY (id_sessao) REFERENCES sessao(id_sessao),
  CONSTRAINT ck_reserva_periodo CHECK (fim_previsto > inicio_previsto),
  CONSTRAINT ck_reserva_status
    CHECK (status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA','CONCLUIDA','CANCELADA')),
  CONSTRAINT ck_reserva_valor CHECK (valor_previsto >= 0),
  CONSTRAINT ck_reserva_percentual CHECK (percentual_entrada_minima BETWEEN 0 AND 100),
  CONSTRAINT ck_reserva_entrada CHECK (valor_entrada_minima IS NULL OR valor_entrada_minima >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 11. PARTICIPANTES DA SESSAO (CHAVE PRIMARIA COMPOSTA)
-- segundos_gratis: parte da hora gratis diaria do Clube usada por este
-- cliente nesta sessao (0 a 3600). A soma por cliente e por dia
-- (pelo horario_inicio da sessao) nao pode passar de 3600: regra da aplicacao.
CREATE TABLE participante_sessao (
  id_sessao       INT NOT NULL,
  id_cliente      INT NOT NULL,
  segundos_gratis INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id_sessao, id_cliente),
  CONSTRAINT ck_participante_gratis CHECK (segundos_gratis <= 3600),
  CONSTRAINT fk_participante_sessao
    FOREIGN KEY (id_sessao) REFERENCES sessao(id_sessao),
  CONSTRAINT fk_participante_cliente
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 12. PAGAMENTO
-- Cada pagamento pertence a exatamente UMA origem: sessao, reserva
-- (entrada) ou assinatura. Uma sessao pode ter varios pagamentos
-- (entrada, saida, tempo acrescentado). Pagamento nunca e apagado.
CREATE TABLE pagamento (
  id_pagamento        INT NOT NULL AUTO_INCREMENT,
  data_hora_pagamento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valor_pago          DECIMAL(10,2) NOT NULL,
  momento_pagamento   VARCHAR(30),
  tipo_pagamento      VARCHAR(20) NOT NULL,
  id_forma_pagamento  INT,
  id_funcionario      INT NOT NULL,
  id_sessao           INT,
  id_reserva          INT,
  id_assinatura       INT,
  PRIMARY KEY (id_pagamento),
  INDEX idx_pagamento_data (data_hora_pagamento),
  CONSTRAINT fk_pagamento_forma
    FOREIGN KEY (id_forma_pagamento) REFERENCES forma_pagamento(id_forma_pagamento),
  CONSTRAINT fk_pagamento_funcionario
    FOREIGN KEY (id_funcionario) REFERENCES funcionario(id_funcionario),
  CONSTRAINT fk_pagamento_sessao
    FOREIGN KEY (id_sessao) REFERENCES sessao(id_sessao),
  CONSTRAINT fk_pagamento_reserva
    FOREIGN KEY (id_reserva) REFERENCES reserva(id_reserva),
  CONSTRAINT fk_pagamento_assinatura
    FOREIGN KEY (id_assinatura) REFERENCES assinatura_clube(id_assinatura),
  CONSTRAINT ck_pagamento_valor CHECK (valor_pago >= 0),
  CONSTRAINT ck_pagamento_forma
    CHECK ((valor_pago = 0 AND id_forma_pagamento IS NULL)
        OR (valor_pago > 0 AND id_forma_pagamento IS NOT NULL)),
  CONSTRAINT ck_pagamento_tipo CHECK (tipo_pagamento IN ('SESSAO','RESERVA','ASSINATURA')),
  CONSTRAINT ck_pagamento_momento
    CHECK (momento_pagamento IS NULL OR momento_pagamento IN ('ENTRADA','SAIDA')),
  CONSTRAINT ck_pagamento_origem
    CHECK ((id_sessao IS NOT NULL) + (id_reserva IS NOT NULL) + (id_assinatura IS NOT NULL) = 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 13. CREDITO DE TEMPO
-- Gerado quando o cliente paga um tempo e nao o usa inteiro (ex.: tempo
-- ABERTO, minimo de 1 h, usou 30 min -> 30 min de credito). Valido por 3 meses.
-- minutos_restantes e o unico atributo de saldo: ao usar o credito,
-- abata-o aqui. Saldo do cliente = SUM(minutos_restantes) dos creditos com
-- valido_ate >= hoje. Credito vencido fica guardado (nada e apagado).
CREATE TABLE credito_tempo (
  id_credito        INT NOT NULL AUTO_INCREMENT,
  id_cliente        INT NOT NULL,
  id_tipo           INT NOT NULL,
  id_sessao_origem  INT NOT NULL,
  minutos_restantes INT NOT NULL,
  data_geracao      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valido_ate        DATE NOT NULL,
  PRIMARY KEY (id_credito),
  INDEX idx_credito_cliente_validade (id_cliente, valido_ate),
  UNIQUE (id_sessao_origem, id_cliente),
  CONSTRAINT fk_credito_cliente
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  CONSTRAINT fk_credito_tipo
    FOREIGN KEY (id_tipo) REFERENCES tipo_equipamento(id_tipo),
  CONSTRAINT fk_credito_sessao
    FOREIGN KEY (id_sessao_origem) REFERENCES sessao(id_sessao),
  CONSTRAINT ck_credito_minutos CHECK (minutos_restantes >= 0),
  CONSTRAINT ck_credito_validade CHECK (valido_ate > DATE(data_geracao))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Conferir menores sem responsavel (regra aplicada pelo sistema;
-- o banco nao consegue verificar entre tabelas sem trigger).
CREATE VIEW vw_menor_sem_responsavel AS
SELECT c.id_cliente, c.nome, c.data_nascimento
  FROM cliente AS c
  LEFT JOIN responsavel AS r ON r.id_cliente = c.id_cliente
 WHERE c.data_nascimento > CURDATE() - INTERVAL 18 YEAR
   AND r.id_responsavel IS NULL;
