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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Consultas de contrato do Clube (em dia, renovação) e do saldo de crédito. */
@Tag("db")
class ClubeCreditoTest {
    private final AssinaturaDAO assinaturas = new AssinaturaDAO();
    private final CreditoDAO creditos = new CreditoDAO();
    private final LocalDate hoje = LocalDate.now();
    private Connection c;
    private int cli;

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        c = app();
        cli = BancoTeste.cliente(c, "Ana", "11144477735");
    }

    @Test
    void emDiaSoParaVigente() throws SQLException {
        int vencida = BancoTeste.cliente(c, "Bia", "52998224725");
        int cancelada = BancoTeste.cliente(c, "Caio", "39053344705");
        BancoTeste.assinatura(c, cli, hoje.minusDays(5), hoje.plusDays(10), "ATIVA");
        BancoTeste.assinatura(c, vencida, hoje.minusMonths(2), hoje.minusDays(1), "ATIVA");
        BancoTeste.assinatura(c, cancelada, hoje.minusDays(5), hoje.plusDays(10), "CANCELADA");
        assertTrue(assinaturas.emDia(c, cli, hoje));
        assertTrue(assinaturas.emDia(c, cli, hoje.plusDays(10)), "último dia ainda está em dia");
        assertFalse(assinaturas.emDia(c, vencida, hoje));
        assertFalse(assinaturas.emDia(c, cancelada, hoje));
    }

    @Test
    void renovacaoEmDiaContinuaDoFim() throws SQLException {
        int id = BancoTeste.assinatura(c, cli, LocalDate.of(2026, 11, 1), hoje.plusDays(5), "ATIVA");
        assinaturas.renovar(c, id, hoje);
        assertEquals(hoje.plusDays(6).plusMonths(1).minusDays(1), assinaturas.dataFim(c, id));
    }

    @Test
    void renovacaoVencidaRecomecaDeHoje() throws SQLException {
        int id = BancoTeste.assinatura(c, cli, hoje.minusMonths(3), hoje.minusDays(20), "ATIVA");
        assinaturas.renovar(c, id, hoje);
        assertEquals(hoje.plusMonths(1).minusDays(1), assinaturas.dataFim(c, id));
    }

    @Test
    void saldoContaSoCreditoDentroDaValidade() throws SQLException {
        int func = BancoTeste.funcionario(c, "atd", "ATENDENTE", true);
        int equip = BancoTeste.equipamento(c, "PC01", "PC");
        int tipo = BancoTeste.idTipo(c, "PC");
        LocalDateTime t = LocalDateTime.now().minusMonths(4).withNano(0);
        int s1 = BancoTeste.sessaoAtiva(c, "ABERTO", t, null, equip, func, cli);
        Sql.atualizar(c, "UPDATE sessao SET horario_fim = ?, status_sessao = 'ENCERRADA' WHERE id_sessao = ?", t.plusMinutes(20), s1);
        creditos.gerar(c, cli, tipo, s1, 40, t.plusMinutes(20));            // vencido
        int s2 = BancoTeste.sessaoAtiva(c, "ABERTO", LocalDateTime.now().minusMinutes(30).withNano(0), null, equip, func, cli);
        creditos.gerar(c, cli, tipo, s2, 30, LocalDateTime.now().withNano(0)); // válido
        assertEquals(30, creditos.saldo(c, cli, hoje));
        assertEquals(70, creditos.saldo(c, cli, hoje.minusMonths(4)), "na época os dois valiam");
        assertEquals(2, Sql.contar(c, "SELECT COUNT(*) FROM credito_tempo WHERE id_cliente = ?", cli), "vencido fica guardado");
    }
}
