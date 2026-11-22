package geeks.dados;

import geeks.BancoTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static geeks.BancoTeste.app;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O banco recusa dados inválidos mesmo que a aplicação tente gravar (conexão do geeks_app). */
@Tag("db")
class RestricoesBancoTest {
    private Connection c;
    private int func;
    private int equip;
    private int cli;
    private final LocalDateTime agora = LocalDateTime.now().withNano(0);

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        c = app();
        func = BancoTeste.funcionario(c, "atd", "ATENDENTE", true);
        equip = BancoTeste.equipamento(c, "PC01", "PC");
        cli = BancoTeste.cliente(c, "Ana", "11144477735");
    }

    private void recusa(String sql, Object... params) {
        SQLException e = assertThrows(SQLException.class, () -> Sql.atualizar(c, sql, params), sql);
        assertTrue(e.getErrorCode() == 3819 || e.getErrorCode() == 1062 || e.getErrorCode() == 1452
                || e.getErrorCode() == 1048 || e.getErrorCode() == 1142 || e.getErrorCode() == 1264
                || e.getErrorCode() == 1406,
                "erro inesperado " + e.getErrorCode() + ": " + e.getMessage());
    }

    @Test
    void segundaSessaoAbertaNoMesmoEquipamentoSoDepoisDeEncerrar() throws SQLException {
        int c2 = BancoTeste.cliente(c, "Bia", "52998224725");
        int s1 = BancoTeste.sessaoAtiva(c, "ABERTO", agora.minusMinutes(10), null, equip, func, cli);
        SQLException e = assertThrows(SQLException.class,
                () -> BancoTeste.sessaoAtiva(c, "ABERTO", agora, null, equip, func, c2));
        assertTrue(e.getMessage().contains("uq_sessao_equipamento_em_uso"));
        Sql.atualizar(c, "UPDATE sessao SET horario_fim = ?, status_sessao = 'ENCERRADA' WHERE id_sessao = ?", agora, s1);
        assertDoesNotThrow(() -> BancoTeste.sessaoAtiva(c, "ABERTO", agora, null, equip, func, c2));
    }

    @Test
    void segundaAssinaturaAtivaSoDepoisDeCancelar() throws SQLException {
        LocalDate hoje = LocalDate.now();
        int a1 = BancoTeste.assinatura(c, cli, hoje, hoje.plusMonths(1), "ATIVA");
        SQLException e = assertThrows(SQLException.class,
                () -> BancoTeste.assinatura(c, cli, hoje, hoje.plusMonths(1), "ATIVA"));
        assertTrue(e.getMessage().contains("uq_assinatura_ativa"));
        Sql.atualizar(c, "UPDATE assinatura_clube SET situacao = 'CANCELADA' WHERE id_assinatura = ?", a1);
        assertDoesNotThrow(() -> BancoTeste.assinatura(c, cli, hoje, hoje.plusMonths(1), "ATIVA"));
    }

    @Test
    void cpfRepetidoOuForaDoFormato() {
        SQLException e = assertThrows(SQLException.class, () -> BancoTeste.cliente(c, "Outra", "11144477735"));
        assertTrue(e.getMessage().contains("uq_cliente_cpf"));
        recusa("INSERT INTO cliente (nome, cpf, data_nascimento) VALUES ('X', '123.456.789-09', CURDATE())");
        recusa("INSERT INTO cliente (nome, cpf, data_nascimento) VALUES ('X', '1234', CURDATE())");
        recusa("INSERT INTO responsavel (id_cliente, nome, cpf) VALUES (?, 'Resp', 'abc')", cli);
    }

    @Test
    void horariosEStatusIncoerentesDaSessao() {
        String ins = "INSERT INTO sessao (horario_inicio, horario_previsto, horario_fim, tipo_tempo, status_sessao, "
                + "valor_hora_contratado, id_equipamento, id_funcionario) VALUES (?, ?, ?, ?, ?, 10.00, ?, ?)";
        // fim antes do início
        recusa(ins, agora, null, agora.minusMinutes(5), "ABERTO", "ENCERRADA", equip, func);
        // encerrada sem fim / ativa com fim
        recusa(ins, agora, null, null, "ABERTO", "ENCERRADA", equip, func);
        recusa(ins, agora, null, agora.plusMinutes(5), "ABERTO", "EM_ANDAMENTO", equip, func);
        // FECHADO sem previsto / ABERTO com previsto
        recusa(ins, agora, null, null, "FECHADO", "EM_ANDAMENTO", equip, func);
        recusa(ins, agora, agora.plusHours(1), null, "ABERTO", "EM_ANDAMENTO", equip, func);
    }

    @Test
    void horaGratisAcimaDe3600() throws SQLException {
        int s = BancoTeste.sessaoAtiva(c, "ABERTO", agora, null, equip, func, cli);
        recusa("UPDATE participante_sessao SET segundos_gratis = 3601 WHERE id_sessao = ?", s);
        assertDoesNotThrow(() -> Sql.atualizar(c, "UPDATE participante_sessao SET segundos_gratis = 3600 WHERE id_sessao = ?", s));
    }

    @Test
    void pagamentoFormaEOrigem() throws SQLException {
        int s = BancoTeste.sessaoAtiva(c, "ABERTO", agora, null, equip, func, cli);
        int dinheiro = (int) Sql.contar(c, "SELECT id_forma_pagamento FROM forma_pagamento WHERE nome = 'Dinheiro'");
        String ins = "INSERT INTO pagamento (valor_pago, tipo_pagamento, id_forma_pagamento, id_funcionario, id_sessao, id_reserva) "
                + "VALUES (?, 'SESSAO', ?, ?, ?, ?)";
        recusa(ins, new java.math.BigDecimal("10.00"), null, func, s, null);   // > 0 sem forma
        recusa(ins, new java.math.BigDecimal("0.00"), dinheiro, func, s, null); // 0 com forma
        recusa(ins, new java.math.BigDecimal("5.00"), dinheiro, func, null, null); // sem origem
        int r = Sql.inserir(c, "INSERT INTO reserva (inicio_previsto, fim_previsto, valor_previsto, id_cliente, id_equipamento) "
                + "VALUES (?, ?, 10.00, ?, ?)", agora.plusDays(1), agora.plusDays(1).plusHours(1), cli, equip);
        recusa(ins, new java.math.BigDecimal("5.00"), dinheiro, func, s, r);    // duas origens
        recusa(ins, new java.math.BigDecimal("-1.00"), dinheiro, func, s, null); // negativo
        assertDoesNotThrow(() -> Sql.atualizar(c, ins, new java.math.BigDecimal("0.00"), null, func, s, null));
        assertDoesNotThrow(() -> Sql.atualizar(c, ins, new java.math.BigDecimal("10.00"), dinheiro, func, s, null));
    }

    @Test
    void creditoNegativoOuValidadeAntesDaGeracao() throws SQLException {
        int s = BancoTeste.sessaoAtiva(c, "ABERTO", agora, null, equip, func, cli);
        int tipo = BancoTeste.idTipo(c, "PC");
        String ins = "INSERT INTO credito_tempo (id_cliente, id_tipo, id_sessao_origem, minutos_restantes, data_geracao, valido_ate) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        recusa(ins, cli, tipo, s, -5, agora, agora.toLocalDate().plusMonths(3));
        recusa(ins, cli, tipo, s, 30, agora, agora.toLocalDate().minusDays(1));
        recusa(ins, cli, tipo, s, 30, agora, agora.toLocalDate());
        assertDoesNotThrow(() -> Sql.atualizar(c, ins, cli, tipo, s, 30, agora, agora.toLocalDate().plusMonths(3)));
    }

    @Test
    void valoresNegativos() {
        recusa("UPDATE tipo_equipamento SET valor_hora = -1 WHERE nome = 'PC'");
        recusa("INSERT INTO reserva (inicio_previsto, fim_previsto, valor_previsto, id_cliente, id_equipamento) "
                + "VALUES (?, ?, -10.00, ?, ?)", agora.plusDays(1), agora.plusDays(1).plusHours(1), cli, equip);
        recusa("INSERT INTO reserva (inicio_previsto, fim_previsto, valor_previsto, id_cliente, id_equipamento) "
                + "VALUES (?, ?, 10.00, ?, ?)", agora.plusDays(1), agora.plusDays(1), cli, equip);
    }

    @Test
    void appNaoApagaClienteSessaoNemPagamentoMasApagaReserva() throws SQLException {
        int s = BancoTeste.sessaoAtiva(c, "ABERTO", agora, null, equip, func, cli);
        Sql.atualizar(c, "INSERT INTO pagamento (valor_pago, tipo_pagamento, id_funcionario, id_sessao) VALUES (0, 'SESSAO', ?, ?)", func, s);
        for (String tabela : new String[]{"cliente", "sessao", "pagamento", "participante_sessao", "funcionario"}) {
            SQLException e = assertThrows(SQLException.class, () -> Sql.atualizar(c, "DELETE FROM " + tabela));
            assertEquals(1142, e.getErrorCode(), "DELETE deveria ser negado em " + tabela);
        }
        int r = Sql.inserir(c, "INSERT INTO reserva (inicio_previsto, fim_previsto, valor_previsto, id_cliente, id_equipamento) "
                + "VALUES (?, ?, 10.00, ?, ?)", agora.plusDays(1), agora.plusDays(1).plusHours(1), cli, equip);
        assertEquals(1, Sql.atualizar(c, "DELETE FROM reserva WHERE id_reserva = ?", r));
    }

    @Test
    void appNaoAlteraEstrutura() {
        SQLException e = assertThrows(SQLException.class, () -> Sql.atualizar(c, "DROP TABLE reserva"));
        assertEquals(1142, e.getErrorCode());
        assertThrows(SQLException.class, () -> Sql.atualizar(c, "ALTER TABLE cliente ADD COLUMN x INT"));
    }
}
