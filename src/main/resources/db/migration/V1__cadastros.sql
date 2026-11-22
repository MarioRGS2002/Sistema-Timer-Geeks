-- ============================================================
-- GEEKS - V1: tabelas de cadastro
-- cliente, responsavel, tipo_equipamento, equipamento, funcionario,
-- plano_clube, forma_pagamento
-- Requer MySQL 8.0.16+ (CHECK aplicado). InnoDB, utf8mb4.
-- NUNCA editar esta migration depois de aplicada: mudanças vão em V5, V6...
-- ============================================================

-- 1. CLIENTE
-- CPF opcional (RN001). Cliente nunca e apagado: ativo = 0.
CREATE TABLE cliente (
  id_cliente      INT NOT NULL AUTO_INCREMENT,
  nome            VARCHAR(100) NOT NULL,
  apelido         VARCHAR(50),
  cpf             CHAR(11),
  data_nascimento DATE NOT NULL,
  telefone        VARCHAR(20),
  data_cadastro   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ativo           TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_cliente),
  CONSTRAINT uq_cliente_cpf UNIQUE (cpf),
  INDEX idx_cliente_nome (nome),
  CONSTRAINT ck_cliente_cpf CHECK (cpf IS NULL OR cpf REGEXP '^[0-9]{11}$')
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2. RESPONSAVEL
-- Recebe o id do cliente. Um responsavel com varios filhos aparece
-- uma vez por cliente (mesmo CPF, id_cliente diferente).
CREATE TABLE responsavel (
  id_responsavel INT NOT NULL AUTO_INCREMENT,
  id_cliente     INT NOT NULL,
  nome           VARCHAR(100) NOT NULL,
  cpf            CHAR(11) NOT NULL,
  telefone       VARCHAR(20),
  parentesco     VARCHAR(50),
  PRIMARY KEY (id_responsavel),
  CONSTRAINT uq_responsavel_cliente_cpf UNIQUE (id_cliente, cpf),
  CONSTRAINT fk_responsavel_cliente
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  CONSTRAINT ck_responsavel_cpf CHECK (cpf REGEXP '^[0-9]{11}$')
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 3. TIPO DE EQUIPAMENTO
CREATE TABLE tipo_equipamento (
  id_tipo           INT NOT NULL AUTO_INCREMENT,
  nome              VARCHAR(50) NOT NULL,
  descricao         VARCHAR(255),
  valor_hora        DECIMAL(10,2) NOT NULL,
  max_participantes INT NOT NULL DEFAULT 1,
  PRIMARY KEY (id_tipo),
  UNIQUE (nome),
  CONSTRAINT ck_tipo_valor_hora CHECK (valor_hora >= 0),
  CONSTRAINT ck_tipo_max_participantes CHECK (max_participantes >= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 4. EQUIPAMENTO
CREATE TABLE equipamento (
  id_equipamento INT NOT NULL AUTO_INCREMENT,
  identificador  VARCHAR(50) NOT NULL,
  id_tipo        INT NOT NULL,
  data_aquisicao DATE,
  status         VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',
  PRIMARY KEY (id_equipamento),
  UNIQUE (identificador),
  CONSTRAINT fk_equipamento_tipo
    FOREIGN KEY (id_tipo) REFERENCES tipo_equipamento(id_tipo),
  CONSTRAINT ck_equipamento_status
    CHECK (status IN ('DISPONIVEL','EM_USO','INDISPONIVEL'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 5. FUNCIONARIO (antiga Atendente)
-- senha_hash guarda BCrypt (nunca senha em texto). ativo = 0 em vez de excluir.
CREATE TABLE funcionario (
  id_funcionario INT NOT NULL AUTO_INCREMENT,
  nome           VARCHAR(100) NOT NULL,
  login          VARCHAR(50) NOT NULL,
  senha_hash     VARCHAR(255) NOT NULL,
  cargo          VARCHAR(20) NOT NULL,
  data_admissao  DATE,
  turno_trabalho VARCHAR(20),
  salario        DECIMAL(10,2),
  ativo          TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_funcionario),
  UNIQUE (login),
  CONSTRAINT ck_funcionario_cargo CHECK (cargo IN ('ATENDENTE','GERENTE')),
  CONSTRAINT ck_funcionario_salario CHECK (salario IS NULL OR salario >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 6. PLANO DO CLUBE
CREATE TABLE plano_clube (
  id_plano            INT NOT NULL AUTO_INCREMENT,
  nome_plano          VARCHAR(50) NOT NULL,
  valor_mensalidade   DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id_plano),
  UNIQUE (nome_plano),
  CONSTRAINT ck_plano_valor CHECK (valor_mensalidade >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 8. FORMA DE PAGAMENTO
CREATE TABLE forma_pagamento (
  id_forma_pagamento INT NOT NULL AUTO_INCREMENT,
  nome               VARCHAR(50) NOT NULL,
  descricao          VARCHAR(255),
  ativo              TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_forma_pagamento),
  UNIQUE (nome)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
