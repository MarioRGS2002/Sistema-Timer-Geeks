package geeks.model;

import geeks.BancoTeste;
import geeks.dados.Banco;
import geeks.dados.CreditoDAO;
import geeks.dados.Sql;
import geeks.dados.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static geeks.BancoTeste.app;
import static geeks.BancoTeste.decimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Casos de encerramento de sessão gravados no banco (cobrança, hora grátis, crédito, rollback). */
@Tag("db")
class SessaoECobrancaBancoTest {
    private GeeksModel model;
    private int func;
    private int pc1;
    private int pc2;
    private int ps5;
    private int cli;
    private final LocalDate ontem = LocalDate.now().minusDays(1);

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        func = BancoTeste.logar("atd", "ATENDENTE").getIdFuncionario();
        try (Connection c = app()) {
            pc1 = BancoTeste.equipamento(c, "PC01", "PC");
            pc2 = BancoTeste.equipamento(c, "PC02", "PC");
            ps5 = BancoTeste.equipamento(c, "Ps5", "PlayStation");
            cli = BancoTeste.cliente(c, "Gabriel Dantas", "11144477735");
        }
        model = new GeeksModel();
    }

    private int sessao(String tipo, LocalDateTime ini, LocalDateTime previsto, int equip) throws SQLException {
        try (Connection c = app()) {
            return BancoTeste.sessaoAtiva(c, tipo, ini, previsto, equip, func, cli);
        }
    }

    private void encerrar(int idSessao, LocalDateTime fim) {
        Transacao.executar(c -> {
            model.encerrar(c, idSessao, fim, func);
            return null;
        });
    }

    private BigDecimal pago(int idSessao) throws SQLException {
        try (Connection c = app()) {
            return decimal(c, "SELECT COALESCE(SUM(valor_pago), 0) FROM pagamento WHERE id_sessao = ?", idSessao);
        }
    }

    private long contar(String sql, Object... p) throws SQLException {
        try (Connection c = app()) {
            return Sql.contar(c, sql, p);
        }
    }

    private void clubeEmDia() throws SQLException {
        try (Connection c = app()) {
            BancoTeste.assinatura(c, cli, LocalDate.now().minusDays(20), LocalDate.now().plusDays(10), "ATIVA");
        }
    }

    @Test
    void semClube() throws SQLException {
        LocalDateTime ini = LocalDateTime.of(ontem, LocalTime.of(18, 0));
        int s = sessao("FECHADO", ini, ini.plusHours(2), pc1);
        encerrar(s, ini.plusHours(2));
        assertEquals(new BigDecimal("20.00"), pago(s));
        assertEquals(0, contar("SELECT COUNT(*) FROM pagamento WHERE id_sessao = ? AND valor_pago = 0", s));
        assertEquals(1, contar("SELECT COUNT(*) FROM sessao WHERE id_sessao = ? AND status_sessao = 'ENCERRADA' "
                + "AND valor_total_devido = 20.00", s));
        assertEquals(1, contar("SELECT COUNT(*) FROM equipamento WHERE id_equipamento = ? AND status = 'DISPONIVEL'", pc1));
    }

    @Test
    void clubeHoraGratisInteira() throws SQLException {
        clubeEmDia();
        LocalDateTime ini = LocalDateTime.of(ontem, LocalTime.of(14, 0));
        int s = sessao("FECHADO", ini, ini.plusMinutes(90), pc1);
        encerrar(s, ini.plusMinutes(90));
        assertEquals(3600, contar("SELECT segundos_gratis FROM participante_sessao WHERE id_sessao = ?", s));
        assertEquals(new BigDecimal("5.00"), pago(s));
        // a parte grátis vira pagamento de valor 0 SEM forma de pagamento
        assertEquals(1, contar("SELECT COUNT(*) FROM pagamento WHERE id_sessao = ? AND valor_pago = 0 "
                + "AND id_forma_pagamento IS NULL", s));
    }

    @Test
    void horaGratisParcialNoMesmoDia() throws SQLException {
        clubeEmDia();
        LocalDateTime a = LocalDateTime.of(ontem, LocalTime.of(10, 0));
        int s1 = sessao("FECHADO", a, a.plusMinutes(40), pc1);
        encerrar(s1, a.plusMinutes(40));
        LocalDateTime b = LocalDateTime.of(ontem, LocalTime.of(15, 0));
        int s2 = sessao("FECHADO", b, b.plusMinutes(60), pc2);
        encerrar(s2, b.plusMinutes(60));
        assertEquals(2400, contar("SELECT segundos_gratis FROM participante_sessao WHERE id_sessao = ?", s1));
        assertEquals(1200, contar("SELECT segundos_gratis FROM participante_sessao WHERE id_sessao = ?", s2));
        assertEquals(new BigDecimal("0.00"), pago(s1));            // totalmente grátis
        assertEquals(new BigDecimal("6.67"), pago(s2));            // 40 min pagos
        try (Connection c = app()) {
            assertEquals(3600, new geeks.dados.SessaoDAO().gratisUsadoNoDia(c, cli, ontem));
        }
    }

    @Test
    void horaGratisVaiPeloDiaDoInicioMesmoCruzandoMeiaNoite() throws SQLException {
        clubeEmDia();
        // começou 23:30 de anteontem e terminou 00:30 de ontem: a hora grátis é do dia do INÍCIO
        LocalDateTime ini = LocalDateTime.of(ontem.minusDays(1), LocalTime.of(23, 30));
        int s1 = sessao("FECHADO", ini, ini.plusMinutes(60), pc1);
        encerrar(s1, ini.plusMinutes(60));
        assertEquals(3600, contar("SELECT segundos_gratis FROM participante_sessao WHERE id_sessao = ?", s1));
        // no dia seguinte (ontem) a hora grátis está inteira de novo
        LocalDateTime b = LocalDateTime.of(ontem, LocalTime.of(9, 0));
        int s2 = sessao("FECHADO", b, b.plusMinutes(30), pc2);
        encerrar(s2, b.plusMinutes(30));
        assertEquals(1800, contar("SELECT segundos_gratis FROM participante_sessao WHERE id_sessao = ?", s2));
        try (Connection c = app()) {
            assertEquals(3600, new geeks.dados.SessaoDAO().gratisUsadoNoDia(c, cli, ontem.minusDays(1)));
            assertEquals(1800, new geeks.dados.SessaoDAO().gratisUsadoNoDia(c, cli, ontem));
        }
    }

    @Test
    void abertaCom30MinutosPagaUmaHoraEGeraCredito() throws SQLException {
        LocalDateTime ini = LocalDateTime.of(ontem, LocalTime.of(19, 0));
        int s = sessao("ABERTO", ini, null, ps5);
        encerrar(s, ini.plusMinutes(30));
        assertEquals(new BigDecimal("10.00"), pago(s));
        assertEquals(30, contar("SELECT minutos_restantes FROM credito_tempo WHERE id_sessao_origem = ?", s));
        assertEquals(1, contar("SELECT COUNT(*) FROM credito_tempo WHERE id_sessao_origem = ? AND valido_ate = ?",
                s, ontem.plusMonths(CreditoDAO.MESES_VALIDADE)));
        assertEquals(1, contar("SELECT COUNT(*) FROM credito_tempo WHERE id_sessao_origem = ? AND id_tipo = "
                + "(SELECT id_tipo FROM tipo_equipamento WHERE nome = 'PlayStation')", s));
    }

    @Test
    void precoCongeladoDepoisDeMudarOValorDoTipo() throws SQLException {
        LocalDateTime ini = LocalDateTime.of(ontem, LocalTime.of(10, 0));
        int s = sessao("FECHADO", ini, ini.plusHours(1), pc1);
        try (Connection c = BancoTeste.migrator()) {
            Sql.atualizar(c, "UPDATE tipo_equipamento SET valor_hora = 99.00 WHERE nome = 'PC'");
        }
        encerrar(s, ini.plusHours(1));
        assertEquals(new BigDecimal("10.00"), pago(s));
    }

    @Test
    void falhaNoEncerramentoDesfazTudo() throws SQLException {
        LocalDateTime ini = LocalDateTime.of(ontem, LocalTime.of(19, 0));
        int s = sessao("ABERTO", ini, null, ps5);
        // funcionário inexistente: o INSERT do pagamento falha DEPOIS de atualizar sessão e participante
        assertThrows(RuntimeException.class, () -> Transacao.executar(c -> {
            model.encerrar(c, s, ini.plusMinutes(30), 999_999);
            return null;
        }));
        assertEquals(1, contar("SELECT COUNT(*) FROM sessao WHERE id_sessao = ? AND horario_fim IS NULL "
                + "AND status_sessao = 'EM_ANDAMENTO' AND valor_total_devido IS NULL", s));
        assertEquals(0, contar("SELECT segundos_gratis FROM participante_sessao WHERE id_sessao = ?", s));
        assertEquals(0, contar("SELECT COUNT(*) FROM pagamento WHERE id_sessao = ?", s));
        assertEquals(0, contar("SELECT COUNT(*) FROM credito_tempo WHERE id_sessao_origem = ?", s));
    }

    @Test
    void fluxoDaTelaAbrirEEncerrarPeloModel() throws SQLException {
        Cliente cliente = model.getClientes().get(0);
        Dispositivo d = model.getDispositivos().get(0);
        Sessao s = model.iniciarSessao(cliente, d);
        assertTrue(model.dispositivoOcupado(d));
        assertThrows(IllegalStateException.class, () -> model.iniciarSessao(cliente, model.getDispositivos().get(1)),
                "cliente não pode estar em duas sessões ativas");
        assertNull(model.encerrarSessao(s.getId()));
        assertEquals(0, model.getSessoesAtivas().size());
        assertEquals(1, model.filtrarHistorico(PeriodoHistorico.SEMANA, "").size());
        // aberta e encerrada no mesmo instante (engano): 0 s usados, nada cobrado, nenhum pagamento
        assertEquals(0, contar("SELECT COUNT(*) FROM pagamento WHERE id_sessao = ?", s.getIdSessao()));
        assertEquals(1, contar("SELECT COUNT(*) FROM sessao WHERE id_sessao = ? AND status_sessao = 'ENCERRADA' "
                + "AND valor_total_devido = 0", s.getIdSessao()));
        assertEquals(func, contar("SELECT id_funcionario FROM sessao WHERE id_sessao = ?", s.getIdSessao()));
        Banco.dataSource();
    }
}
