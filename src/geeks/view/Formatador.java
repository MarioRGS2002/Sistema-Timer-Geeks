package geeks.view;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class Formatador {
    private static final Locale BR = new Locale("pt", "BR");

    private Formatador() {
    }

    public static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(BR).format(valor).replace('\u00A0', ' ');
    }

    public static String duracao(Duration d) {
        return d.toHours() + "h " + String.format("%02d", d.toMinutesPart()) + "min";
    }

    public static String cpf(String c) {
        if (c == null || c.length() != 11) {
            return c;
        }
        return c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-" + c.substring(9);
    }

    public static String dataHora(LocalDateTime d) {
        return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public static String dataExtenso(LocalDate d) {
        String dia = d.getDayOfWeek().getDisplayName(TextStyle.FULL, BR);
        return maiuscula(dia) + ", " + d.getDayOfMonth() + " de " + d.getMonth().getDisplayName(TextStyle.FULL, BR);
    }

    public static String mes(YearMonth m) {
        return maiuscula(m.getMonth().getDisplayName(TextStyle.FULL, BR)) + " " + m.getYear();
    }

    private static String maiuscula(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
