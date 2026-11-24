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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static geeks.BancoTeste.app;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Erros relatados no teste com o cliente e os achados da revisão de código. */
@Tag("db")
class ErrosRelatadosTest {
    private GeeksModel model;
    private int func;
    private int pc1;
    private int cli;

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        func = BancoTeste.logar("atd", "ATENDENTE").getIdFuncionario();
        try (Connection c = app()) {
            pc1 = BancoTeste.equipamento(c, "PC01", "PC");
            BancoTeste.equipamento(c, "PC02", "PC");
            cli = BancoTeste.cliente(c, "Ana Souza", "11144477735");
        }
        model = new GeeksModel();
    }

    @AfterEach
    void sair() {
        Contexto.sair();
    }

    private Cliente cliente(int id) {
        return model.getClientes().stream().filter(x -> x.getIdCliente() == id).findFirst().orElseThrow();
    }

    private Dispositivo pc1() {
        return model.getDispositivos().stream().filter(d -> d.getIdEquipamento() == pc1).findFirst().orElseThrow();
    }

    @Test
    void telefoneDoResponsavelNaoPodeSerODoCliente() {
        String menor = "01/01/" + (java.time.LocalDate.now().getYear() - 10);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> model.cadastrarCliente("Lucas", menor,
                "(11) 91234-5678", "52998224725", "Fernanda", "12345678909", "11912345678", "Mãe"));
        assertTrue(e.getMessage().contains("telefone"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> model.cadastrarCliente("Lucas", menor,
                "1234", "52998224725", "Fernanda", "12345678909", "", "Mãe"), "telefone sem DDD");
    }

    @Test
    void encerramentoGravaOValorMostradoNaJanela() throws SQLException {
        LocalDateTime ini = LocalDateTime.now().minusMinutes(95).truncatedTo(ChronoUnit.SECONDS);
        int s;
        try (Connection c = app()) {
            s = BancoTeste.sessaoAtiva(c, "ABERTO", ini, null, pc1, func, cli);
        }
        PreviaEncerramento mostrada = model.previaEncerramento("S" + s, false);
        // o atendente demora 2 minutos para clicar: o valor gravado é o mostrado, não o recalculado
        LocalDateTime fimMostrado = mostrada.getFim().minusMinutes(2);
        PreviaEncerramento antes = new PreviaEncerramento(Cobranca.calcular(TipoTempo.ABERTO, ini, null, fimMostrado,
                new BigDecimal("10.00"), 0), 0, BigDecimal.ZERO.setScale(2), fimMostrado);
        PreviaEncerramento gravada = model.encerrarSessao("S" + s, "PIX", false, fimMostrado);
        assertEquals(antes.getAPagar(), gravada.getAPagar());
        try (Connection c = app()) {
            assertEquals(1, Sql.contar(c, "SELECT COUNT(*) FROM sessao WHERE id_sessao = ? AND horario_fim = ? "
                    + "AND valor_total_devido = ?", s, fimMostrado, antes.getAPagar()));
        }
    }

    @Test
    void reservaDeOutroClienteJaComecadaBloqueiaASessao() throws SQLException {
        int outro;
        try (Connection c = app()) {
            outro = BancoTeste.cliente(c, "Bia", "52998224725");
            LocalDateTime comecou = LocalDateTime.now().minusMinutes(10).withNano(0);
            Sql.inserir(c, "INSERT INTO reserva (inicio_previsto, fim_previsto, valor_previsto, id_cliente, id_equipamento) "
                    + "VALUES (?, ?, 10.00, ?, ?)", comecou, comecou.plusHours(1), outro, pc1);
        }
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> model.iniciarSessao(cliente(cli), pc1(), null));
        assertTrue(e.getMessage().contains("reserva"), e.getMessage());
        assertTrue(e.getMessage().contains("PC02"), "sugere outro do mesmo tipo: " + e.getMessage());
        // o dono da reserva entra normalmente e a reserva vira a sessão
        assertNotNull(model.iniciarSessao(cliente(outro), pc1(), null));
    }

    @Test
    void fichaDoClienteCompleta() {
        String menor = "02/03/" + (java.time.LocalDate.now().getYear() - 12);
        Cliente c = model.cadastrarCliente("Lucas", menor, "(11) 3456-7890", "52998224725", "Fernanda", "12345678909",
                "(11) 91234-5678", "Mãe");
        model.assinarPlano(c, Plano.CLUBE, "PIX");
        FichaCliente f = model.getFicha(c.getIdCliente());
        assertEquals("Fernanda", f.getRespNome());
        assertEquals("Mãe", f.getRespParentesco());
        assertEquals("12345678909", f.getRespCpf());
        assertEquals("(11) 91234-5678", f.getRespTelefone());
        assertNotNull(f.getClubeAte());
        assertNotNull(f.getCadastro());
        assertEquals(0, f.getSessoes());
        assertNull(model.getFicha(999_999));
    }

    @Test
    void buscaDoClubeDevolveTodosOsHomonimos() throws SQLException {
        try (Connection c = app()) {
            BancoTeste.cliente(c, "Ana Lima", "52998224725");
        }
        assertEquals(2, model.buscarClientes("ana").size());
        assertEquals(1, model.buscarClientes("111.444.777-35").size(), "por CPF: exato");
        assertEquals(0, model.buscarClientes("ninguem").size());
    }

    @Test
    void avisoDeTempoAcabandoApareceUmaVezSo() {
        Sessao s = model.iniciarSessao(cliente(cli), pc1(), 15);
        assertEquals(0, model.sessoesParaAvisar(5).size(), "faltam 15 min: ainda não");
        assertEquals(1, model.sessoesParaAvisar(Regras.ALERTA_RESERVA_MINUTOS + 10).size());
        assertEquals(0, model.sessoesParaAvisar(Regras.ALERTA_RESERVA_MINUTOS + 10).size(), "já avisado (vale para os outros PCs)");
        assertTrue(s.getPrevisto() != null);
    }

    @Test
    void reservaChegandoEntraNoAviso() {
        model.registrarReserva(cliente(cli), pc1(), LocalDateTime.now().plusMinutes(5), 60, null, false);
        assertEquals(1, model.reservasChegando(10).size());
        assertEquals(0, model.reservasChegando(2).size());
    }
}
