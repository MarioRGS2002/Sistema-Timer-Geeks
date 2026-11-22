package geeks.dados;

import geeks.BancoTeste;
import geeks.model.Contexto;
import geeks.model.FaturamentoModel;
import geeks.model.LoginModel;
import geeks.model.PeriodoFaturamento;
import geeks.model.SerieFaturamento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static geeks.BancoTeste.app;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Somas do faturamento, histórico (ordem e LIKE) e regras de acesso por cargo. */
@Tag("db")
class FaturamentoHistoricoSegurancaTest {
    private final PagamentoDAO pagamentos = new PagamentoDAO();
    private final SessaoDAO sessoes = new SessaoDAO();
    private final LocalDate dia = LocalDate.now().minusDays(3);
    private int func;
    private int dinheiro;
    private int pix;
    private int s1;

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        try (Connection c = app()) {
            func = BancoTeste.funcionario(c, "atd", "ATENDENTE", true);
            BancoTeste.funcionario(c, "chefe", "GERENTE", true);
            BancoTeste.funcionario(c, "antigo", "ATENDENTE", false);
            int pc = BancoTeste.equipamento(c, "PC01", "PC");
            int sw = BancoTeste.equipamento(c, "Nintendo Switch", "Nintendo Switch");
            int joao = BancoTeste.cliente(c, "João Aragão", "11144477735");
            int ana = BancoTeste.cliente(c, "Ana", "52998224725");
            dinheiro = pagamentos.idForma(c, "Dinheiro");
            pix = pagamentos.idForma(c, "PIX");
            LocalDateTime t1 = LocalDateTime.of(dia, LocalTime.of(10, 0));
            s1 = encerrada(c, t1, pc, joao);
            int s2 = encerrada(c, t1.plusHours(5), sw, ana);
            pagamentos.registrar(c, t1.plusHours(1), new BigDecimal("10.00"), "SAIDA", "SESSAO", dinheiro, func, s1, null, null);
            pagamentos.registrar(c, t1.plusHours(1).plusMinutes(30), new BigDecimal("5.00"), "SAIDA", "SESSAO", pix, func, s1, null, null);
            pagamentos.registrar(c, t1.plusHours(6), new BigDecimal("0.00"), "SAIDA", "SESSAO", null, func, s2, null, null);
            pagamentos.registrar(c, t1.plusHours(6), new BigDecimal("7.50"), "SAIDA", "SESSAO", pix, func, s2, null, null);
            // 00:00 do dia seguinte NÃO pertence ao dia anterior
            pagamentos.registrar(c, dia.plusDays(1).atStartOfDay(), new BigDecimal("100.00"), "SAIDA", "SESSAO", dinheiro, func, s2, null, null);
        }
    }

    @AfterEach
    void sair() {
        Contexto.sair();
    }

    private int encerrada(Connection c, LocalDateTime ini, int equip, int cli) throws SQLException {
        int s = BancoTeste.sessaoAtiva(c, "FECHADO", ini, ini.plusHours(1), equip, func, cli);
        sessoes.encerrar(c, s, ini.plusHours(1), new BigDecimal("10.00"));
        return s;
    }

    @Test
    void somasPorHoraDiaEForma() throws SQLException {
        LocalDateTime ini = dia.atStartOfDay();
        LocalDateTime fim = dia.plusDays(1).atStartOfDay();
        try (Connection c = app()) {
            assertEquals(new BigDecimal("22.50"), pagamentos.total(c, ini, fim));
            Map<Integer, BigDecimal> hora = pagamentos.porHora(c, ini, fim);
            assertEquals(Map.of(11, new BigDecimal("15.00"), 16, new BigDecimal("7.50")), hora, "horas vazias ficam ausentes");
            Map<LocalDate, BigDecimal> porDia = pagamentos.porDia(c, ini, fim.plusDays(1));
            assertEquals(new BigDecimal("22.50"), porDia.get(dia));
            assertEquals(new BigDecimal("100.00"), porDia.get(dia.plusDays(1)));
            assertFalse(porDia.containsKey(dia.minusDays(1)));
            Map<String, BigDecimal> forma = pagamentos.porForma(c, ini, fim);
            assertEquals(new BigDecimal("10.00"), forma.get("Dinheiro"));
            assertEquals(new BigDecimal("12.50"), forma.get("PIX"));
            assertEquals(new BigDecimal("0.00"), forma.get(null), "hora grátis aparece sem forma");
        }
    }

    @Test
    void historicoOrdenadoELikeSemAcentoEMaiusculas() throws SQLException {
        try (Connection c = app()) {
            List<SessaoDAO.Linha> todas = sessoes.historico(c, dia.atStartOfDay(), "");
            assertEquals(2, todas.size());
            assertFalse(todas.get(0).fim.isBefore(todas.get(1).fim), "ordem horario_fim DESC");
            assertEquals(s1, sessoes.historico(c, dia.atStartOfDay(), "JOAO ARAGAO").get(0).idSessao);
            assertEquals(1, sessoes.historico(c, dia.atStartOfDay(), "pc0").size());
            assertEquals(1, sessoes.historico(c, dia.atStartOfDay(), "nintendo").size());
            assertEquals(0, sessoes.historico(c, dia.atStartOfDay(), "100%").size(), "% é literal no LIKE");
        }
    }

    @Test
    void atendenteNaoVeFaturamentoGerenteVe() {
        new LoginModel().autenticar("atd", "senha-teste-123".toCharArray());
        assertThrows(SecurityException.class, () -> new FaturamentoModel().serie(PeriodoFaturamento.SEMANA, dia));
        new LoginModel().autenticar("chefe", "senha-teste-123".toCharArray());
        SerieFaturamento serie = new FaturamentoModel().serie(PeriodoFaturamento.SEMANA, dia);
        assertNotNull(serie);
        assertEquals(new BigDecimal("22.50"), serie.getValores().get(dia.getDayOfWeek().getValue() - 1));
    }

    @Test
    void loginRecusaSenhaErradaInativoEInexistente() {
        LoginModel login = new LoginModel();
        assertNull(login.autenticar("atd", "errada".toCharArray()));
        assertNull(login.autenticar("antigo", "senha-teste-123".toCharArray()), "funcionário inativo não entra");
        assertNull(login.autenticar("ninguem", "senha-teste-123".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> login.autenticar("", new char[0]));
        assertNotNull(login.autenticar("ATD ", "senha-teste-123".toCharArray()));
    }

    @Test
    void semLoginNadaQueGraveFuncionarioFunciona() {
        Contexto.sair();
        assertThrows(SecurityException.class, Contexto::idFuncionario);
        assertThrows(SecurityException.class, () -> new FaturamentoModel().serie(PeriodoFaturamento.DIA, dia));
    }
}
