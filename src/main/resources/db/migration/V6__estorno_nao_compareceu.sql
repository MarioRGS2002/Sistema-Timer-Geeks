-- ============================================================
-- GEEKS - V6: estorno de pagamento, reserva "não compareceu" e
--             correção do horário de saída de sessão esquecida
-- NUNCA editar esta migration depois de aplicada.
-- ============================================================

-- 14. ESTORNO
-- Pagamento nunca é apagado nem alterado. Um pagamento lançado errado é
-- estornado (total ou parcialmente) por um GERENTE, com motivo. O faturamento
-- soma os pagamentos e subtrai os estornos (na data do estorno).
-- A soma dos estornos de um pagamento não passa do valor dele: regra da
-- aplicação (com SELECT ... FOR UPDATE no pagamento).
CREATE TABLE estorno (
  id_estorno     INT NOT NULL AUTO_INCREMENT,
  id_pagamento   INT NOT NULL,
  valor          DECIMAL(10,2) NOT NULL,
  motivo         VARCHAR(200) NOT NULL,
  data_hora      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_funcionario INT NOT NULL,
  PRIMARY KEY (id_estorno),
  INDEX idx_estorno_data (data_hora),
  CONSTRAINT fk_estorno_pagamento
    FOREIGN KEY (id_pagamento) REFERENCES pagamento(id_pagamento),
  CONSTRAINT fk_estorno_funcionario
    FOREIGN KEY (id_funcionario) REFERENCES funcionario(id_funcionario),
  CONSTRAINT ck_estorno_valor CHECK (valor > 0),
  CONSTRAINT ck_estorno_motivo CHECK (CHAR_LENGTH(TRIM(motivo)) >= 3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Reserva em que o cliente não veio: não é cancelamento (a entrada paga fica).
ALTER TABLE reserva DROP CHECK ck_reserva_status;
ALTER TABLE reserva ADD CONSTRAINT ck_reserva_status
  CHECK (status_reserva IN ('PENDENTE_ENTRADA','CONFIRMADA','CONCLUIDA','CANCELADA','NAO_COMPARECEU'));

-- Sessão esquecida aberta: o gerente encerra informando o horário real de
-- saída; fica registrado quem corrigiu.
ALTER TABLE sessao
  ADD COLUMN fim_corrigido_por INT NULL AFTER id_funcionario,
  ADD CONSTRAINT fk_sessao_fim_corrigido
    FOREIGN KEY (fim_corrigido_por) REFERENCES funcionario(id_funcionario);
