package geeks.model;

import geeks.dados.RelatorioDAO;
import geeks.dados.Transacao;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Relatórios simples do período do Faturamento (somente GERENTE). */
public class RelatoriosModel {
    private static final int LIMITE = 5;
    private final RelatorioDAO dao = new RelatorioDAO();

    /** Uma linha de relatório: o que (ex.: "PC01") e o detalhe (ex.: "12 sessões · 18h30min"). */
    public record Linha(String rotulo, String detalhe) {
    }

    /** Os três relatórios do período. */
    public record Relatorio(List<Linha> equipamentos, List<Linha> pico, List<Linha> clientes) {
    }

    public Relatorio periodo(PeriodoFaturamento periodo, LocalDate referencia) {
        Contexto.exigirGerente();
        LocalDate ini = periodo.inicio(referencia);
        LocalDate fim = periodo.inicio(periodo.deslocar(referencia, 1));
        LocalDateTime a = ini.atStartOfDay();
        LocalDateTime b = fim.atStartOfDay();
        return Transacao.consultar(c -> {
            List<Linha> equip = new ArrayList<>();
            for (Object[] r : dao.equipamentosMaisUsados(c, a, b, LIMITE)) {
                long seg = ((Number) r[2]).longValue();
                equip.add(new Linha((String) r[0], sessoes(r[1]) + "  ·  " + duracao(seg)));
            }
            List<Linha> pico = new ArrayList<>();
            for (Object[] r : dao.horariosDePico(c, a, b, LIMITE)) {
                int h = ((Number) r[0]).intValue();
                pico.add(new Linha(String.format("%02dh às %02dh", h, (h + 1) % 24), sessoes(r[1])));
            }
            List<Linha> clientes = new ArrayList<>();
            for (Object[] r : dao.clientesFrequentes(c, a, b, LIMITE)) {
                BigDecimal v = new BigDecimal(r[2].toString()).setScale(2, java.math.RoundingMode.HALF_UP);
                clientes.add(new Linha((String) r[0], sessoes(r[1]) + "  ·  R$ " + v.toPlainString().replace('.', ',')));
            }
            return new Relatorio(equip, pico, clientes);
        });
    }

    private static String sessoes(Object n) {
        long q = ((Number) n).longValue();
        return q + (q == 1 ? " sessão" : " sessões");
    }

    private static String duracao(long segundos) {
        Duration d = Duration.ofSeconds(segundos);
        return d.toHours() + "h" + String.format("%02d", d.toMinutesPart()) + "min";
    }
}
