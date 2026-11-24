package geeks.model;

import geeks.BancoTeste;
import geeks.dados.Sql;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Estorno e caixa, mais tempo, não compareceu, editar cliente e sessão esquecida aberta. */
@Tag("db")
class DiaADiaTest {
    private GeeksModel model;
    private final CaixaModel caixa = new CaixaModel();
    private int atd;
    private int pc1;
    private int cli;

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        try (Connection c = app()) {
            BancoTeste.funcionario(c, "chefe", "GERENTE", true);
            pc1 = BancoTeste.equipamento(c, "PC01", "PC");
            BancoTeste.equipamento(c, "PC02", "PC");
            cli = BancoTeste.cliente(c, "Ana Souza", "11144477735");
        }
        atd = BancoTeste.logar("atd", "ATENDENTE").getIdFuncionario();
        model = new GeeksModel();
    }

    @AfterEach
    void sair() {
        Contexto.sair();
    }

    private void comoGerente() {
        new LoginModel().autenticar("chefe", "senha-teste-123".toCharArray());
    }

    private Cliente ana() {
        return model.getClientes().stream().filter(x -> x.getIdCliente() == cli).findFirst().orElseThrow();
    }

    private Dispositivo pc1() {
        return model.getDispositivos().stream().filter(d -> d.getIdEquipamento() == pc1).findFirst().orElseThrow();
    }

    private long contar(String sql, Object... p) throws SQLException {
        try (Connection c = app()) {
            return Sql.contar(c, sql, p);
        }
    }

    /** Sessão aberta há {@code minutos} e encerrada agora, paga em PIX. */
    private int sessaoPaga(int minutos) throws SQLException {
        LocalDateTime ini = LocalDateTime.now().minusMinutes(minutos).truncatedTo(ChronoUnit.SECONDS);
        int s;
        try (Connection c = app()) {
            s = BancoTeste.sessaoAtiva(c, "ABERTO", ini, null, pc1, atd, cli);
        }
        model.encerrarSessao("S" + s, "PIX", false);
        return s;
    }

    // ---------- estorno e caixa ----------

    @Test
    void estornoParcialDescontaDoCaixaENaoPassaDoValor() throws SQLException {
        int s = sessaoPaga(120); // R$ 20,00
        int idPag = (int) contar("SELECT id_pagamento FROM pagamento WHERE id_sessao = ? AND valor_pago > 0", s);
        assertThrows(SecurityException.class, () -> caixa.estornar(idPag, new BigDecimal("5.00"), "cobrado errado"), "atendente não estorna");
        comoGerente();
        assertThrows(IllegalArgumentException.class, () -> caixa.estornar(idPag, new BigDecimal("5.00"), "x"), "motivo curto");
        caixa.estornar(idPag, new BigDecimal("5.00"), "cobrou 30 min a mais");
        CaixaModel.Dia dia = caixa.dia(LocalDate.now());
        assertEquals(new BigDecimal("15.00"), dia.total);
        assertEquals(new BigDecimal("15.00"), dia.porForma.get("PIX"));
        assertEquals(new BigDecimal("5.00"), dia.totalEstornos());
        assertEquals(new BigDecimal("5.00"), dia.pagamentos.get(0).estornado());
        assertThrows(IllegalArgumentException.class, () -> caixa.estornar(idPag, new BigDecimal("15.01"), "passa do valor"));
        assertDoesNotThrow(() -> caixa.estornar(idPag, new BigDecimal("15.00"), "devolvido o resto"));
        assertEquals(new BigDecimal("0.00"), caixa.dia(LocalDate.now()).total);
        assertEquals(1, contar("SELECT COUNT(*) FROM pagamento WHERE id_pagamento = ? AND valor_pago = 20.00", idPag),
                "o pagamento original não é alterado");
    }

    @Test
    void atendenteNaoVeFaturamentoNemCaixa() {
        assertThrows(SecurityException.class, () -> model.getFaturamentoHoje());
        assertThrows(SecurityException.class, () -> caixa.dia(LocalDate.now()));
        comoGerente();
        assertDoesNotThrow(() -> model.getFaturamentoHoje());
    }

    // ---------- mais tempo ----------

    @Test
    void maisTempoSoNoFechadoEContaDoFimCombinado() throws SQLException {
        Sessao s = model.iniciarSessao(ana(), pc1(), 30);
        LocalDateTime novo = model.acrescentarTempo(s.getId(), 60);
        assertEquals(s.getPrevisto().plusMinutes(60), novo);
        assertEquals(1, contar("SELECT COUNT(*) FROM sessao WHERE id_sessao = ? AND horario_previsto = ? AND notificacao_enviada = 0",
                s.getIdSessao(), novo));
        Cliente bia = model.cadastrarCliente("Bia", "01/01/1990", "", "52998224725", "", "", "", "");
        Sessao livre = model.iniciarSessao(bia, model.getDispositivos().stream().filter(d -> d.getIdEquipamento() != pc1)
                .findFirst().orElseThrow(), null);
        assertThrows(IllegalStateException.class, () -> model.acrescentarTempo(livre.getId(), 30), "tempo livre não precisa");
    }

    @Test
    void maisTempoRecusaSeBaterNaReservaDeOutro() throws SQLException {
        Sessao s = model.iniciarSessao(ana(), pc1(), 30);
        try (Connection c = app()) {
            int outro = BancoTeste.cliente(c, "Bia", "52998224725");
            LocalDateTime ini = s.getPrevisto().plusMinutes(90);
            Sql.inserir(c, "INSERT INTO reserva (inicio_previsto, fim_previsto, valor_previsto, id_cliente, id_equipamento) "
                    + "VALUES (?, ?, 10.00, ?, ?)", ini, ini.plusHours(1), outro, pc1);
        }
        assertDoesNotThrow(() -> model.acrescentarTempo(s.getId(), 30), "fim + 30 + folga de 60 = exatamente o início");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> model.acrescentarTempo(s.getId(), 30));
        assertTrue(e.getMessage().contains("reserva"), e.getMessage());
    }

    // ---------- não compareceu ----------

    @Test
    void naoCompareceuSoDepoisDoHorarioEGuardaOStatus() throws SQLException {
        Reserva futura = model.registrarReserva(ana(), pc1(), LocalDateTime.now().plusHours(3), 60, null, false);
        assertThrows(IllegalStateException.class, () -> model.marcarNaoCompareceu(futura));
        int idPassada;
        LocalDateTime passou = LocalDateTime.now().minusMinutes(20).withNano(0);
        try (Connection c = app()) {
            idPassada = Sql.inserir(c, "INSERT INTO reserva (inicio_previsto, fim_previsto, valor_previsto, id_cliente, id_equipamento) "
                    + "VALUES (?, ?, 10.00, ?, ?)", passou, passou.plusHours(1), cli, pc1);
        }
        model.marcarNaoCompareceu(new Reserva(idPassada, ana(), pc1(), passou, passou.plusHours(1)));
        assertEquals(1, contar("SELECT COUNT(*) FROM reserva WHERE id_reserva = ? AND status_reserva = 'NAO_COMPARECEU'", idPassada));
        assertTrue(model.getReservasDoDia(LocalDate.now()).stream().noneMatch(r -> r.getIdReserva() == idPassada));
    }

    // ---------- editar cliente ----------

    @Test
    void editarClienteMudaDadosMasNaoOCpf() throws SQLException {
        String menor = "10/05/" + (LocalDate.now().getYear() - 15);
        Cliente c = model.atualizarCliente(cli, "Ana Souza Lima", menor, "(11) 3456-7890", "Carla", "12345678909", "", "Mãe");
        assertEquals("Ana Souza Lima", c.getNome());
        assertEquals("11144477735", c.getCpf());
        assertEquals("Carla", c.getResponsavel());
        model.atualizarCliente(cli, "Ana Souza Lima", menor, "(11) 3456-7890", "Carla Lima", "12345678909", "", "Mãe");
        assertEquals(1, contar("SELECT COUNT(*) FROM responsavel WHERE id_cliente = ?", cli), "atualiza, não duplica o responsável");
        assertThrows(IllegalArgumentException.class,
                () -> model.atualizarCliente(cli, "Ana", menor, "", "", "", "", ""), "menor sem responsável");
        assertThrows(IllegalArgumentException.class,
                () -> model.atualizarCliente(cli, "Ana", "31/02/2000", "", "", "", "", ""), "data inválida");
    }

    // ---------- sessão esquecida aberta ----------

    @Test
    void sessaoEsquecidaGeraAvisoESoOGerenteCorrigeASaida() throws SQLException {
        LocalDateTime ini = LocalDateTime.now().minusHours(Regras.SESSAO_LONGA_HORAS + 1).truncatedTo(ChronoUnit.SECONDS);
        int s;
        try (Connection c = app()) {
            s = BancoTeste.sessaoAtiva(c, "ABERTO", ini, null, pc1, atd, cli);
        }
        assertEquals(1, model.sessoesParaAvisar(10).size(), "tempo livre aberto há muitas horas");
        LocalDateTime saida = ini.plusMinutes(70);
        assertThrows(SecurityException.class, () -> model.previaEncerramento("S" + s, false, saida), "atendente não corrige");
        assertThrows(SecurityException.class, () -> model.encerrarSessao("S" + s, "PIX", false, saida));
        comoGerente();
        assertThrows(IllegalArgumentException.class, () -> model.previaEncerramento("S" + s, false, ini.minusMinutes(5)),
                "antes do início");
        PreviaEncerramento p = model.previaEncerramento("S" + s, false, saida);
        assertEquals(new BigDecimal("11.67"), p.getAPagar(), "70 min em vez de 7 horas");
        model.encerrarSessao("S" + s, "Dinheiro", false, p.getFim());
        assertEquals(1, contar("SELECT COUNT(*) FROM sessao AS s INNER JOIN funcionario AS f ON f.id_funcionario = s.fim_corrigido_por "
                + "WHERE s.id_sessao = ? AND f.login = 'chefe' AND s.horario_fim = ?", s, saida));
        // o dinheiro foi recebido agora: entra no caixa de HOJE, mesmo com a saída corrigida para horas atrás
        assertEquals(new BigDecimal("11.67"), caixa.dia(LocalDate.now()).total);
    }
}
