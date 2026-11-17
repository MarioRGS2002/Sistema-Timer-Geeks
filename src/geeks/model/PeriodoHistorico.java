package geeks.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum PeriodoHistorico {
    SEMANA("Esta semana"),
    MES("Este mês"),
    TRES_MESES("Últimos 3 meses"),
    ANO("Este ano");

    private final String rotulo;

    PeriodoHistorico(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Primeiro dia (inclusive) do período, a partir de hoje. */
    public LocalDate inicio(LocalDate hoje) {
        switch (this) {
            case SEMANA:
                return hoje.with(DayOfWeek.MONDAY);
            case MES:
                return hoje.withDayOfMonth(1);
            case TRES_MESES:
                return hoje.minusMonths(3);
            default:
                return hoje.withDayOfYear(1);
        }
    }
}
