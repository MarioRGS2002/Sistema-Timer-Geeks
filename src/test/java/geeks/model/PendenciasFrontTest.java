package geeks.model;

import geeks.BancoTeste;
import geeks.dados.Sql;
import geeks.dados.Transacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static geeks.BancoTeste.app;
import static geeks.BancoTeste.decimal;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As 9 pendências do front, agora resolvidas: regras gravadas no banco. */
@Tag("db")
class PendenciasFrontTest {
    private GeeksModel model;
    private int func;
    private int pc1;
    private int pc2;
    private int cli;
    private int tipoPc;

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        func = BancoTeste.logar("atd", "ATENDENTE").getIdFuncionario();
        try (Connection c = app()) {
            pc1 = BancoTeste.equipamento(c, "PC01", "PC");
            pc2 = BancoTeste.equipamento(c, "PC02", "PC");
            cli = BancoTeste.cliente(c, "Gabriel", "11144477735");
            tipoPc = BancoTeste.idTipo(c, "PC");
        }
        model = new GeeksModel();
    }

    @AfterEach
    void sair() {
        Contexto.sair();
    }

    private long contar(String sql, Object... p) throws SQLException {
        try (Connection c = app()) {
            return Sql.contar(c, sql, p);
        }
    }

    private BigDecimal valor(String sql, Object... p) throws SQLException {
        try (Connection c = app()) {
            return decimal(c, sql, p);
        }
    }

    private Cliente gabriel() {
        return model.getClientes().stream().filter(x -> x.getIdCliente() == cli).findFirst().orElseThrow();
    }

    private Dispositivo disp(int id) {
        return model.getDispositivos().stream().filter(d -> d.getIdEquipamento() == id).findFirst().orElseThrow();
    }

    // ---------- 2 e 3: data de nascimento e responsável ----------

    @Test
    void cadastroGravaNascimentoEResponsavelCompleto() throws SQLException {
        LocalDate nasc = LocalDate.now().minusYears(12).minusDays(3);
        String txt = String.format("%02d/%02d/%d", nasc.getDayOfMonth(), nasc.getMonthValue(), nasc.getYear());
        Cliente c = model.cadastrarCliente("Lucas", txt, "", "52998224725", "Fernanda", "123.456.789-09", "(11) 9999-0000", "Mãe");
        assertEquals(nasc, c.getDataNascimento());
        assertEquals("Fernanda", c.getResponsavel());
        assertEquals(1, contar("SELECT COUNT(*) FROM responsavel WHERE id_cliente = ? AND cpf = '12345678909' "
                + "AND parentesco = 'Mãe' AND telefone = '(11) 9999-0000'", c.getIdCliente()));
        assertEquals(0, contar("SELECT COUNT(*) FROM vw_menor_sem_responsavel"));
    }

    @Test
    void cadastroRecusaDataInvalidaEMenorSemResponsavel() {
        assertThrows(IllegalArgumentException.class,
                () -> model.cadastrarCliente("X", "31/02/2010", "", "52998224725", "", "", "", ""), "31/02 não existe");
        assertThrows(IllegalArgumentException.class,
                () -> model.cadastrarCliente("X", "01/01/2999", "", "52998224725", "", "", "", ""), "futuro");
        String menor = "01/01/" + (LocalDate.now().getYear() - 10);
        assertThrows(IllegalArgumentException.class,
                () -> model.cadastrarCliente("X", menor, "", "52998224725", "", "", "", ""), "menor sem responsável");
        assertThrows(IllegalArgumentException.class,
                () -> model.cadastrarCliente("X", menor, "", "52998224725", "Mãe", "111", "", ""), "CPF do responsável");
        assertDoesNotThrow(() -> model.cadastrarCliente("Adulto", "15031990", "", "52998224725", "", "", "", ""), "ddmmaaaa");
    }

    // ---------- 4: tempo fechado ----------

    @Test
    void sessaoFechadaGravaHorarioPrevistoECobraOContratado() throws SQLException {
        Sessao s = model.iniciarSessao(gabriel(), disp(pc1), 120);
        assertNotNull(s);
        assertEquals(1, contar("SELECT COUNT(*) FROM sessao WHERE id_sessao = ? AND tipo_tempo = 'FECHADO' "
                + "AND TIMESTAMPDIFF(MINUTE, horario_inicio, horario_previsto) = 120", s.getIdSessao()));
        LocalDateTime fim = s.getInicio().plusMinutes(30);
        PreviaEncerramento r = Transacao.executar(c -> model.encerrar(c, s.getIdSessao(), fim, func, "PIX", false));
        assertEquals(new BigDecimal("20.00"), r.getCobranca().getValor(), "paga as 2 horas contratadas");
        assertEquals(90, r.getCobranca().getMinutosCredito(), "o que não usou vira crédito");
        assertEquals(1, contar("SELECT COUNT(*) FROM pagamento AS p INNER JOIN forma_pagamento AS f "
                + "ON f.id_forma_pagamento = p.id_forma_pagamento WHERE p.id_sessao = ? AND f.nome = 'PIX' AND p.valor_pago = 20.00",
                s.getIdSessao()), "1: forma de pagamento escolhida");
    }

    // ---------- 5 e 9: usar o crédito ----------

    @Test
    void usarCreditoAbateOTempoEOSaldoDoQueVencePrimeiro() throws SQLException {
        LocalDateTime antes = LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS);
        try (Connection c = app()) {
            int s1 = BancoTeste.sessaoAtiva(c, "ABERTO", antes, null, pc2, func, cli);
            Sql.atualizar(c, "UPDATE sessao SET horario_fim = ?, status_sessao = 'ENCERRADA' WHERE id_sessao = ?", antes.plusMinutes(10), s1);
            new geeks.dados.CreditoDAO().gerar(c, cli, tipoPc, s1, 50, antes.plusMinutes(10));
        }
        LocalDateTime ini = LocalDateTime.now().minusMinutes(40).truncatedTo(ChronoUnit.SECONDS);
        int s;
        try (Connection c = app()) {
            s = BancoTeste.sessaoAtiva(c, "ABERTO", ini, null, pc1, func, cli);
        }
        PreviaEncerramento sem = model.previaEncerramento("S" + s, false);
        assertEquals(50, sem.getSaldoCreditoMinutos());
        assertEquals(new BigDecimal("10.00"), sem.getAPagar(), "sem usar crédito: mínimo de 1 hora");
        LocalDateTime fim = ini.plusMinutes(40);
        PreviaEncerramento r = Transacao.executar(c -> model.encerrar(c, s, fim, func, "Dinheiro", true));
        assertEquals(2400, r.getCobranca().getSegundosCredito());
        assertEquals(new BigDecimal("0.00"), r.getAPagar(), "40 min cobertos pelo crédito");
        assertEquals(10, contar("SELECT COALESCE(SUM(minutos_restantes), 0) FROM credito_tempo WHERE id_cliente = ?", cli));
        assertEquals(2400, contar("SELECT segundos_credito FROM participante_sessao WHERE id_sessao = ?", s));
        assertEquals(0, contar("SELECT COUNT(*) FROM pagamento WHERE id_sessao = ?", s), "nada a pagar, nenhum pagamento");
    }

    // ---------- 6, 7 e 8: reserva ----------

    @Test
    void reservaComDuracaoEntradaPagaQueEDescontadaNaSessao() throws SQLException {
        LocalDateTime daqui10 = LocalDateTime.now().plusMinutes(10).truncatedTo(ChronoUnit.MINUTES);
        Reserva res = model.registrarReserva(gabriel(), disp(pc1), daqui10, 90, "Débito", false);
        assertEquals(daqui10.plusMinutes(90), res.getFim(), "6: duração escolhida");
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE id_reserva = ? AND status_reserva = 'CONFIRMADA' "
                + "AND valor_previsto = 15.00 AND valor_entrada_minima = 7.50", res.getIdReserva()));
        assertEquals(new BigDecimal("7.50"), valor("SELECT SUM(valor_pago) FROM pagamento WHERE id_reserva = ? "
                + "AND momento_pagamento = 'ENTRADA' AND tipo_pagamento = 'RESERVA'", res.getIdReserva()), "7: entrada no caixa");

        // o cliente chega: a reserva vira a sessão (tempo fechado até o fim da reserva)
        Sessao s = model.iniciarSessao(gabriel(), disp(pc1), null);
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE id_reserva = ? AND id_sessao = ? AND status_reserva = 'CONCLUIDA'",
                res.getIdReserva(), s.getIdSessao()));
        assertEquals(1, contar("SELECT COUNT(*) FROM sessao WHERE id_sessao = ? AND tipo_tempo = 'FECHADO' AND horario_previsto = ?",
                s.getIdSessao(), res.getFim()));
        LocalDateTime fim = s.getInicio().plusMinutes(30);
        PreviaEncerramento r = Transacao.executar(c -> model.encerrar(c, s.getIdSessao(), fim, func, "PIX", false));
        assertEquals(new BigDecimal("7.50"), r.getEntradaPaga());
        assertEquals(r.getCobranca().getValor().subtract(new BigDecimal("7.50")), r.getAPagar(), "entrada descontada");
        assertEquals(r.getAPagar(), valor("SELECT SUM(valor_pago) FROM pagamento WHERE id_sessao = ?", s.getIdSessao()));
    }

    @Test
    void avisoDeSessaoLivreSoBloqueiaSeNaoConfirmar() throws SQLException {
        Cliente outro = model.cadastrarCliente("Bia", "01/01/1990", "", "52998224725", "", "", "", "");
        model.iniciarSessao(outro, disp(pc1), null); // tempo livre agora
        LocalDateTime logo = LocalDateTime.now().plusMinutes(20);
        AvisoReserva aviso = assertThrows(AvisoReserva.class, () -> model.registrarReserva(gabriel(), disp(pc1), logo, 60, null, false));
        assertTrue(aviso.getMessage().contains("PC02"), "sugere outro do mesmo tipo");
        assertDoesNotThrow(() -> model.registrarReserva(gabriel(), disp(pc1), logo, 60, null, true), "8: confirmou o aviso");
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE id_cliente = ? AND status_reserva = 'PENDENTE_ENTRADA'", cli),
                "sem entrada paga fica pendente");
    }

    @Test
    void sessaoFechadaBloqueiaReservaAntesDoFimMaisAFolga() throws SQLException {
        Cliente outro = model.cadastrarCliente("Bia", "01/01/1990", "", "52998224725", "", "", "", "");
        model.iniciarSessao(outro, disp(pc1), 60);
        LocalDateTime cedo = LocalDateTime.now().plusMinutes(90); // fim da sessão + 30 min < folga de 60
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> model.registrarReserva(gabriel(), disp(pc1), cedo, 60, null, true));
        assertTrue(e.getMessage().contains("em uso até"), e.getMessage());
        assertDoesNotThrow(() -> model.registrarReserva(gabriel(), disp(pc1), LocalDateTime.now().plusMinutes(125), 60, null, false));
    }

    // ---------- 1: forma de pagamento no Clube ----------

    @Test
    void mensalidadeDoClubeNaFormaEscolhida() throws SQLException {
        model.assinarPlano(gabriel(), Plano.CLUBE, "Crédito");
        assertEquals(1, contar("SELECT COUNT(*) FROM pagamento AS p INNER JOIN forma_pagamento AS f "
                + "ON f.id_forma_pagamento = p.id_forma_pagamento WHERE p.tipo_pagamento = 'ASSINATURA' AND f.nome = 'Crédito'"));
        assertEquals(java.util.List.of("Dinheiro", "PIX", "Débito", "Crédito"), model.getFormasPagamento());
    }
}
