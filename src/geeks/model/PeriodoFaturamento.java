package geeks.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum PeriodoFaturamento {
    DIA("Dia"),
    SEMANA("Semana"),
    MES("Mês");

    private final String rotulo;

    PeriodoFaturamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Primeiro dia do período que contém a data de referência. */
    public LocalDate inicio(LocalDate referencia) {
        switch (this) {
            case SEMANA:
                return referencia.with(DayOfWeek.MONDAY);
            case MES:
                return referencia.withDayOfMonth(1);
            default:
                return referencia;
        }
    }

    /** Anda para frente (passos positivos) ou para trás (negativos) neste tipo de período. */
    public LocalDate deslocar(LocalDate referencia, int passos) {
        switch (this) {
            case SEMANA:
                return referencia.plusWeeks(passos);
            case MES:
                return referencia.plusMonths(passos);
            default:
                return referencia.plusDays(passos);
        }
    }
}
