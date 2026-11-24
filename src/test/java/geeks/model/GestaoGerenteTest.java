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
import java.util.LinkedHashMap;
import java.util.Map;

import static geeks.BancoTeste.app;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loja (equipamentos e preços), Clube vencendo, relatórios e vários jogadores na mesma sessão. */
@Tag("db")
class GestaoGerenteTest {
    private GeeksModel model;
    private final LojaModel loja = new LojaModel();
    private int func;
    private int pc1;
    private int ps5;
    private int ana;
    private int bia;
    private int caio;

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        try (Connection c = app()) {
            pc1 = BancoTeste.equipamento(c, "PC01", "PC");
            BancoTeste.equipamento(c, "PC02", "PC");
            ps5 = BancoTeste.equipamento(c, "Ps5", "PlayStation");
            ana = BancoTeste.cliente(c, "Ana", "11144477735");
            bia = BancoTeste.cliente(c, "Bia", "52998224725");
            caio = BancoTeste.cliente(c, "Caio", "39053344705");
        }
        func = BancoTeste.logar("chefe", "GERENTE").getIdFuncionario();
        model = new GeeksModel();
    }

    @AfterEach
    void sair() {
        Contexto.sair();
        // volta o preço padrão em memória (os enums são compartilhados entre os testes)
        TipoDispositivo.PC.definirDoBanco(TipoDispositivo.PC.getIdTipo(), new BigDecimal("10.00"));
    }

    private Cliente cliente(int id) {
        return model.getClientes().stream().filter(x -> x.getIdCliente() == id).findFirst().orElseThrow();
    }

    private Dispositivo disp(int id) {
        return model.getDispositivos().stream().filter(d -> d.getIdEquipamento() == id).findFirst().orElseThrow();
    }

    // ---------- Loja: equipamentos ----------

    @Test
    void manutencaoTiraODispositivoDasTelasEBloqueiaSessao() {
        Dispositivo d = disp(pc1);
        loja.manutencao(pc1, true);
        assertThrows(IllegalStateException.class, () -> model.iniciarSessao(cliente(ana), d), "a tela ainda mostrava");
        model.recarregarDispositivos();
        assertTrue(model.getDispositivos().stream().noneMatch(x -> x.getIdEquipamento() == pc1));
        loja.manutencao(pc1, false);
        model.recarregarDispositivos();
        assertDoesNotThrow(() -> model.iniciarSessao(cliente(ana), disp(pc1)));
        assertThrows(IllegalStateException.class, () -> loja.manutencao(pc1, true), "em uso não vai para manutenção");
    }

    @Test
    void cadastrarEquipamentoEAtendenteNaoMexe() throws SQLException {
        loja.cadastrar("PC05", "PC");
        assertThrows(IllegalArgumentException.class, () -> loja.cadastrar("PC05", "PC"), "repetido");
        assertTrue(loja.equipamentos().stream().anyMatch(e -> e.nome().equals("PC05") && !e.emManutencao()));
        BancoTeste.logar("atd", "ATENDENTE");
        assertThrows(SecurityException.class, () -> loja.cadastrar("PC06", "PC"));
        assertThrows(SecurityException.class, () -> loja.manutencao(pc1, true));
        assertThrows(SecurityException.class, () -> loja.alterarPrecos(Map.of(), BigDecimal.TEN));
    }

    // ---------- Loja: preços ----------

    @Test
    void novoPrecoValeParaSessoesNovasEAsAbertasMantemOAntigo() throws SQLException {
        Sessao antes = model.iniciarSessao(cliente(ana), disp(pc1), 60);
        Map<String, BigDecimal> tipos = new LinkedHashMap<>();
        loja.tipos().forEach(t -> tipos.put(t, new BigDecimal("10.00")));
        tipos.put("PC", new BigDecimal("12.50"));
        loja.alterarPrecos(tipos, new BigDecimal("200.00"));
        assertEquals(new BigDecimal("12.50"), TipoDispositivo.PC.getValorHora(), "as telas já mostram o novo preço");
        assertEquals(new BigDecimal("200.00"), Plano.CLUBE.getValor());
        model.recarregarDispositivos();
        Sessao depois = model.iniciarSessao(cliente(bia), model.getDispositivos().stream()
                .filter(d -> d.getNome().equals("PC02")).findFirst().orElseThrow(), 60);
        try (Connection c = app()) {
            assertEquals(1, Sql.contar(c, "SELECT COUNT(*) FROM sessao WHERE id_sessao = ? AND valor_hora_contratado = 10.00", antes.getIdSessao()));
            assertEquals(1, Sql.contar(c, "SELECT COUNT(*) FROM sessao WHERE id_sessao = ? AND valor_hora_contratado = 12.50", depois.getIdSessao()));
        }
        Map<String, BigDecimal> ruim = new LinkedHashMap<>(tipos);
        ruim.put("PC", new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> loja.alterarPrecos(ruim, new BigDecimal("180.00")));
        loja.alterarPrecos(Map.of("PC", new BigDecimal("10.00")), new BigDecimal("180.00"));
    }

    // ---------- Clube vencendo ----------

    @Test
    void clubeVencendoMostraQuemVencePerto() throws SQLException {
        LocalDate hoje = LocalDate.now();
        try (Connection c = app()) {
            BancoTeste.assinatura(c, ana, hoje.minusDays(27), hoje.plusDays(3), "ATIVA");
            BancoTeste.assinatura(c, bia, hoje.minusDays(10), hoje.plusDays(20), "ATIVA");
            BancoTeste.assinatura(c, caio, hoje.minusDays(40), hoje.minusDays(2), "ATIVA");
        }
        var lista = model.clubeVencendo(7);
        assertEquals(2, lista.size());
        assertEquals("Caio", lista.get(0).cliente().getNome(), "vencido há 2 dias vem primeiro");
        assertEquals(-2, lista.get(0).diasParaVencer());
        assertEquals("Ana", lista.get(1).cliente().getNome());
        assertEquals(hoje.plusDays(3), model.clubeAte(cliente(ana)));
        model.assinarPlano(cliente(ana), Plano.CLUBE, "PIX"); // renova antes de vencer: continua do fim
        assertEquals(hoje.plusDays(4).plusMonths(1).minusDays(1), model.clubeAte(cliente(ana)));
    }

    // ---------- relatórios ----------

    @Test
    void relatoriosDoPeriodo() throws SQLException {
        LocalDateTime ini = LocalDateTime.now().minusHours(3).truncatedTo(ChronoUnit.HOURS);
        try (Connection c = app()) {
            int s1 = BancoTeste.sessaoAtiva(c, "ABERTO", ini, null, pc1, func, ana);
            Sql.atualizar(c, "UPDATE sessao SET horario_fim = ?, status_sessao = 'ENCERRADA', valor_total_devido = 20.00 WHERE id_sessao = ?",
                    ini.plusHours(2), s1);
            int s2 = BancoTeste.sessaoAtiva(c, "ABERTO", ini.plusMinutes(10), null, ps5, func, ana);
            Sql.atualizar(c, "UPDATE sessao SET horario_fim = ?, status_sessao = 'ENCERRADA', valor_total_devido = 10.00 WHERE id_sessao = ?",
                    ini.plusMinutes(70), s2);
        }
        var r = new RelatoriosModel().periodo(PeriodoFaturamento.DIA, LocalDate.now().equals(ini.toLocalDate()) ? LocalDate.now() : ini.toLocalDate());
        assertEquals("PC01", r.equipamentos().get(0).rotulo(), "mais tempo de uso");
        assertTrue(r.equipamentos().get(0).detalhe().contains("2h00min"), r.equipamentos().get(0).detalhe());
        assertEquals("Ana", r.clientes().get(0).rotulo());
        assertTrue(r.clientes().get(0).detalhe().startsWith("2 sessões"), r.clientes().get(0).detalhe());
        assertEquals(1, r.pico().size(), "as duas começaram na mesma hora");
        BancoTeste.logar("atd", "ATENDENTE");
        assertThrows(SecurityException.class, () -> new RelatoriosModel().periodo(PeriodoFaturamento.DIA, LocalDate.now()));
    }

    // ---------- vários jogadores ----------

    @Test
    void amigosJogandoJuntoNoMesmoConsole() throws SQLException {
        Sessao s = model.iniciarSessao(cliente(ana), disp(ps5));
        model.adicionarJogador(s.getId(), cliente(bia));
        model.adicionarJogador(s.getId(), cliente(caio));
        var ativas = model.getSessoesAtivas();
        assertEquals(1, ativas.size(), "a sessão não aparece duplicada");
        assertEquals("Ana", ativas.get(0).getCliente().getNome(), "titular");
        assertEquals(java.util.List.of("Bia", "Caio"), ativas.get(0).getJogadores());
        assertTrue(ativas.get(0).cabeMaisJogador(), "PlayStation aceita 4");
        assertThrows(IllegalStateException.class, () -> model.iniciarSessao(cliente(bia), disp(pc1)),
                "quem está jogando junto não abre outra sessão");
        assertThrows(IllegalStateException.class, () -> model.adicionarJogador(s.getId(), cliente(bia)), "já está");
        Sessao pc = model.iniciarSessao(model.cadastrarCliente("Davi", "01/01/1990", "", "71428793860", "", "", "", ""),
                disp(pc1));
        Cliente eva = model.cadastrarCliente("Eva", "01/01/1990", "", "93541134780", "", "", "", "");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> model.adicionarJogador(pc.getId(), eva));
        assertTrue(e.getMessage().contains("no máximo 1"), e.getMessage());
        model.encerrarSessao(s.getId(), "PIX", false);
        assertNotNull(model.iniciarSessao(cliente(bia), model.getDispositivos().stream()
                .filter(d -> d.getNome().equals("PC02")).findFirst().orElseThrow()), "depois de encerrar, a Bia está livre");
        assertFalse(model.getSessoesAtivas().stream().anyMatch(x -> x.getIdSessao() == s.getIdSessao()));
    }
}
