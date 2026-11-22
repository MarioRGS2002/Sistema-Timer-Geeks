package geeks.model;

import geeks.dados.PagamentoDAO;
import geeks.dados.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Faturamento para os gráficos, a partir dos pagamentos registrados (tabela pagamento).
 * Somente o GERENTE pode consultar (checado aqui, no back-end, além da tela).
 */
public class FaturamentoModel {
    private static final Locale BR = new Locale("pt", "BR");
    private static final int HORA_ABRE = 9;
    private static final int HORA_FECHA = 22;
    private static final String[] DIAS_CURTOS = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};

    private final PagamentoDAO dao = new PagamentoDAO();

    public SerieFaturamento serie(PeriodoFaturamento periodo, LocalDate referencia) {
        Contexto.exigirGerente();
        LocalDateTime agora = LocalDateTime.now();
        List<String> rotulos = new ArrayList<>();
        List<String> completos = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();
        String titulo;

        switch (periodo) {
            case DIA: {
                titulo = nomeDia(referencia) + " de " + nomeMes(referencia);
                Map<Integer, BigDecimal> porHora = Transacao.consultar(c ->
                        dao.porHora(c, referencia.atStartOfDay(), referencia.plusDays(1).atStartOfDay()));
                int primeira = Math.min(HORA_ABRE, porHora.keySet().stream().min(Integer::compare).orElse(HORA_ABRE));
                int ultima = Math.max(HORA_FECHA, porHora.keySet().stream().max(Integer::compare).orElse(HORA_FECHA));
                for (int h = primeira; h <= ultima; h++) {
                    rotulos.add(h + "h");
                    completos.add(h + "h às " + (h + 1) + "h");
                    boolean futuro = referencia.isAfter(agora.toLocalDate())
                            || (referencia.equals(agora.toLocalDate()) && h > agora.getHour());
                    valores.add(futuro ? null : porHora.getOrDefault(h, BigDecimal.ZERO));
                }
                break;
            }
            case SEMANA: {
                LocalDate segunda = periodo.inicio(referencia);
                LocalDate domingo = segunda.plusDays(6);
                titulo = segunda.getMonth() == domingo.getMonth()
                        ? segunda.getDayOfMonth() + " a " + domingo.getDayOfMonth() + " de " + nomeMes(domingo)
                        : segunda.getDayOfMonth() + " de " + nomeMes(segunda) + " a " + domingo.getDayOfMonth() + " de " + nomeMes(domingo);
                Map<LocalDate, BigDecimal> porDia = Transacao.consultar(c ->
                        dao.porDia(c, segunda.atStartOfDay(), domingo.plusDays(1).atStartOfDay()));
                for (int i = 0; i < 7; i++) {
                    LocalDate d = segunda.plusDays(i);
                    rotulos.add(DIAS_CURTOS[i]);
                    completos.add(nomeDia(d));
                    valores.add(d.isAfter(agora.toLocalDate()) ? null : porDia.getOrDefault(d, BigDecimal.ZERO));
                }
                break;
            }
            default: {
                LocalDate primeiro = periodo.inicio(referencia);
                titulo = capitalizar(primeiro.getMonth().getDisplayName(TextStyle.FULL, BR)) + " " + primeiro.getYear();
                Map<LocalDate, BigDecimal> porDia = Transacao.consultar(c ->
                        dao.porDia(c, primeiro.atStartOfDay(), primeiro.plusMonths(1).atStartOfDay()));
                for (int dia = 1; dia <= primeiro.lengthOfMonth(); dia++) {
                    LocalDate d = primeiro.withDayOfMonth(dia);
                    rotulos.add(String.valueOf(dia));
                    completos.add(nomeDia(d));
                    valores.add(d.isAfter(agora.toLocalDate()) ? null : porDia.getOrDefault(d, BigDecimal.ZERO));
                }
            }
        }
        return new SerieFaturamento(periodo, titulo, rotulos, completos, valores);
    }

    private String nomeDia(LocalDate d) {
        return capitalizar(d.getDayOfWeek().getDisplayName(TextStyle.FULL, BR)) + ", " + d.getDayOfMonth();
    }

    private String nomeMes(LocalDate d) {
        return d.getMonth().getDisplayName(TextStyle.FULL, BR);
    }

    private String capitalizar(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
