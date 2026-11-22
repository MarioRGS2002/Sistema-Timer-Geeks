package geeks.dados;

import geeks.BancoTeste;
import geeks.model.Cliente;
import geeks.model.Dispositivo;
import geeks.model.GeeksModel;
import geeks.model.Regras;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static geeks.BancoTeste.app;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Conflito de reservas, janela de folga, equipamentos livres e concorrência. */
@Tag("db")
class ReservaEConcorrenciaTest {
    private static final int J = 60;
    private final ReservaDAO reservas = new ReservaDAO();
    private final EquipamentoDAO equipamentos = new EquipamentoDAO();
    private final LocalDateTime amanha14 = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(14, 0));
    private int func;
    private int pc1;
    private int pc2;
    private int pc3;
    private int cli;
    private int cli2;

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        func = BancoTeste.logar("atd", "ATENDENTE").getIdFuncionario();
        try (Connection c = app()) {
            pc1 = BancoTeste.equipamento(c, "PC01", "PC");
            pc2 = BancoTeste.equipamento(c, "PC02", "PC");
            pc3 = BancoTeste.equipamento(c, "PC03", "PC");
            BancoTeste.equipamento(c, "Ps5", "PlayStation");
            cli = BancoTeste.cliente(c, "Ana", "11144477735");
            cli2 = BancoTeste.cliente(c, "Bia", "52998224725");
        }
    }

    private int reservar(Connection c, int equip, LocalDateTime ini) throws SQLException {
        return reservas.inserir(c, ini, ini.plusHours(1), new BigDecimal("10.00"), new BigDecimal("50.00"),
                new BigDecimal("5.00"), cli, equip);
    }

    @Test
    void conflitoJanelaExataEAlteracaoIgnoraAPropria() throws SQLException {
        try (Connection c = app()) {
            int r = reservar(c, pc1, amanha14); // 14:00-15:00
            // 15:30 está dentro da folga de 60 min depois do fim (15:00 + 60 = 16:00)
            assertEquals(1, reservas.conflitos(c, pc1, 0, amanha14.plusMinutes(90), amanha14.plusMinutes(150), J));
            // 16:00 exatamente no limite da janela: aceito
            assertEquals(0, reservas.conflitos(c, pc1, 0, amanha14.plusHours(2), amanha14.plusHours(3), J));
            // antes: 12:00-13:00 termina exatamente 60 min antes das 14:00: aceito
            assertEquals(0, reservas.conflitos(c, pc1, 0, amanha14.minusHours(2), amanha14.minusHours(1), J));
            // outro equipamento: sem conflito
            assertEquals(0, reservas.conflitos(c, pc2, 0, amanha14, amanha14.plusHours(1), J));
            // alterar a própria reserva para 14:30 não conflita consigo mesma
            assertEquals(0, reservas.conflitos(c, pc1, r, amanha14.plusMinutes(30), amanha14.plusMinutes(90), J));
            // cancelada não conta
            reservas.cancelar(c, r);
            assertEquals(0, reservas.conflitos(c, pc1, 0, amanha14, amanha14.plusHours(1), J));
        }
    }

    @Test
    void livresMesmoTipoNuncaDevolveEquipamentoOcupado() throws SQLException {
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        try (Connection c = app()) {
            BancoTeste.sessaoAtiva(c, "ABERTO", agora.minusMinutes(10), null, pc2, func, cli);     // ABERTA agora
            reservar(c, pc3, amanha14);                                                          // reserva amanhã
            List<String> agoraLivres = equipamentos.livresMesmoTipo(c, pc1, agora, agora.plusHours(1), agora, J);
            assertEquals(List.of("PC03"), agoraLivres, "PC02 tem sessão aberta; Ps5 é de outro tipo");
            List<String> amanhaLivres = equipamentos.livresMesmoTipo(c, pc1, amanha14, amanha14.plusHours(1), agora, J);
            assertEquals(List.of("PC02"), amanhaLivres, "PC03 tem reserva sobreposta");
        }
    }

    @Test
    void reservaProximaComSessaoAbertaAvisaELongeAceita() throws SQLException {
        GeeksModel model = new GeeksModel();
        Cliente ana = model.getClientes().stream().filter(x -> x.getNome().equals("Ana")).findFirst().orElseThrow();
        Dispositivo d1 = model.getDispositivos().get(0);
        try (Connection c = app()) {
            BancoTeste.sessaoAtiva(c, "ABERTO", LocalDateTime.now().minusMinutes(5).withNano(0), null, pc1, func, cli2);
        }
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> model.registrarReserva(ana, d1, LocalDateTime.now().plusMinutes(J - 20)));
        assertTrue(e.getMessage().contains("sem hora para acabar"), e.getMessage());
        assertTrue(e.getMessage().contains("PC02"), "sugere outro equipamento do mesmo tipo: " + e.getMessage());
        assertDoesNotThrow(() -> model.registrarReserva(ana, d1, amanha14));
        assertEquals(1, model.getReservasDoDia(amanha14.toLocalDate()).size());
        assertTrue(model.getDiasComReserva(java.time.YearMonth.from(amanha14)).contains(amanha14.toLocalDate()));
        assertThrows(IllegalStateException.class, () -> model.registrarReserva(ana, d1, amanha14.plusMinutes(30)));
    }

    @Test
    void duasThreadsAbrindoSessaoNoMesmoEquipamentoSoUmaVence() throws Exception {
        LocalDateTime ini = LocalDateTime.now().withNano(0);
        int vitorias = corrida(i -> {
            try (Connection c = app()) {
                BancoTeste.sessaoAtiva(c, "ABERTO", ini, null, pc1, func, i == 0 ? cli : cli2);
                return true;
            } catch (SQLException e) {
                assertTrue(e.getMessage().contains("uq_sessao_equipamento_em_uso"), e.getMessage());
                return false;
            }
        });
        assertEquals(1, vitorias);
    }

    @Test
    void duasReservasSimultaneasNoMesmoEquipamentoSoUmaGrava() throws Exception {
        int vitorias = corrida(i -> {
            try {
                return Transacao.executar(c -> {
                    equipamentos.travar(c, pc1); // SELECT ... FOR UPDATE serializa as duas
                    if (reservas.conflitos(c, pc1, 0, amanha14, amanha14.plusHours(1), Regras.JANELA_MINUTOS) > 0) {
                        return false;
                    }
                    reservar(c, pc1, amanha14);
                    return true;
                });
            } catch (RuntimeException e) {
                return false;
            }
        });
        assertEquals(1, vitorias);
        try (Connection c = app()) {
            assertEquals(1, Sql.contar(c, "SELECT COUNT(*) FROM reserva WHERE id_equipamento = ?", pc1));
        }
    }

    @Test
    void cancelarPeloModelMudaStatusSemApagar() throws SQLException {
        GeeksModel model = new GeeksModel();
        Cliente ana = model.getClientes().get(0);
        var r = model.registrarReserva(ana, model.getDispositivos().get(0), amanha14);
        model.cancelarReserva(r);
        assertTrue(model.getReservasDoDia(amanha14.toLocalDate()).isEmpty());
        try (Connection c = app()) {
            assertEquals(1, Sql.contar(c, "SELECT COUNT(*) FROM reserva WHERE id_reserva = ? AND status_reserva = 'CANCELADA'",
                    r.getIdReserva()));
        }
        assertFalse(model.getDiasComReserva(java.time.YearMonth.from(amanha14)).contains(amanha14.toLocalDate()));
    }

    @FunctionalInterface
    private interface Tarefa {
        boolean executar(int indice) throws Exception;
    }

    private int corrida(Tarefa t) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            Future<Boolean> a = ex.submit((Callable<Boolean>) () -> {
                largada.await();
                return t.executar(0);
            });
            Future<Boolean> b = ex.submit((Callable<Boolean>) () -> {
                largada.await();
                return t.executar(1);
            });
            largada.countDown();
            return (a.get(30, TimeUnit.SECONDS) ? 1 : 0) + (b.get(30, TimeUnit.SECONDS) ? 1 : 0);
        } finally {
            ex.shutdownNow();
        }
    }
}
