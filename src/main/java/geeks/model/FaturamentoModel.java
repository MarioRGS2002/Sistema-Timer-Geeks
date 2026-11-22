package geeks.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Faturamento para os gráficos.
 *
 * <p>ATENÇÃO: por enquanto os valores são de EXEMPLO (gerados de forma estável a partir da data),
 * só para montar a interface. No sistema final virão dos pagamentos registrados.</p>
 */
public class FaturamentoModel {
    private static final Locale BR = new Locale("pt", "BR");
    private static final int HORA_ABRE = 9;
    private static final int HORA_FECHA = 22;
    private static final String[] DIAS_CURTOS = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};

    public SerieFaturamento serie(PeriodoFaturamento periodo, LocalDate referencia) {
        LocalDateTime agora = LocalDateTime.now();
        List<String> rotulos = new ArrayList<>();
        List<String> completos = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();
        String titulo;

        switch (periodo) {
            case DIA:
                titulo = nomeDia(referencia) + " de " + nomeMes(referencia);
                for (int h = HORA_ABRE; h <= HORA_FECHA; h++) {
                    rotulos.add(h + "h");
                    completos.add(h + "h às " + (h + 1) + "h");
                    boolean futuro = referencia.isAfter(agora.toLocalDate())
                            || (referencia.equals(agora.toLocalDate()) && h > agora.getHour());
                    valores.add(futuro ? null : valorHora(referencia, h));
                }
                break;
            case SEMANA:
                LocalDate segunda = periodo.inicio(referencia);
                LocalDate domingo = segunda.plusDays(6);
                titulo = segunda.getMonth() == domingo.getMonth()
                        ? segunda.getDayOfMonth() + " a " + domingo.getDayOfMonth() + " de " + nomeMes(domingo)
                        : segunda.getDayOfMonth() + " de " + nomeMes(segunda) + " a " + domingo.getDayOfMonth() + " de " + nomeMes(domingo);
                for (int i = 0; i < 7; i++) {
                    LocalDate d = segunda.plusDays(i);
                    rotulos.add(DIAS_CURTOS[i]);
                    completos.add(nomeDia(d));
                    valores.add(valorDia(d, agora));
                }
                break;
            default:
                LocalDate primeiro = periodo.inicio(referencia);
                titulo = capitalizar(primeiro.getMonth().getDisplayName(TextStyle.FULL, BR)) + " " + primeiro.getYear();
                for (int dia = 1; dia <= primeiro.lengthOfMonth(); dia++) {
                    LocalDate d = primeiro.withDayOfMonth(dia);
                    rotulos.add(String.valueOf(dia));
                    completos.add(nomeDia(d));
                    valores.add(valorDia(d, agora));
                }
        }
        return new SerieFaturamento(periodo, titulo, rotulos, completos, valores);
    }

    private BigDecimal valorDia(LocalDate dia, LocalDateTime agora) {
        if (dia.isAfter(agora.toLocalDate())) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (int h = HORA_ABRE; h <= HORA_FECHA; h++) {
            if (dia.isBefore(agora.toLocalDate()) || h <= agora.getHour()) {
                total = total.add(valorHora(dia, h));
            }
        }
        return total;
    }

    /** Valor de EXEMPLO: mais movimento à tarde/noite e nos fins de semana. */
    private BigDecimal valorHora(LocalDate dia, int hora) {
        Random r = new Random(dia.toEpochDay() * 100L + hora);
        double pico = hora < 12 ? 0.5 : hora < 16 ? 0.8 : hora < 20 ? 1.3 : 0.9;
        double fimDeSemana = dia.getDayOfWeek().getValue() >= 6 ? 1.35 : 1.0;
        double valor = (18 + r.nextInt(30)) * pico * fimDeSemana;
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
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
