package geeks.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Pontos de um gráfico de faturamento: um rótulo e um valor (null = ainda sem dados) por ponto. */
public class SerieFaturamento {
    private final PeriodoFaturamento periodo;
    private final String titulo;
    private final List<String> rotulos;
    private final List<String> rotulosCompletos;
    private final List<BigDecimal> valores;

    public SerieFaturamento(PeriodoFaturamento periodo, String titulo, List<String> rotulos,
                            List<String> rotulosCompletos, List<BigDecimal> valores) {
        this.periodo = periodo;
        this.titulo = titulo;
        this.rotulos = rotulos;
        this.rotulosCompletos = rotulosCompletos;
        this.valores = valores;
    }

    public PeriodoFaturamento getPeriodo() {
        return periodo;
    }

    public String getTitulo() {
        return titulo;
    }

    public List<String> getRotulos() {
        return rotulos;
    }

    public List<String> getRotulosCompletos() {
        return rotulosCompletos;
    }

    public List<BigDecimal> getValores() {
        return valores;
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : valores) {
            if (v != null) {
                total = total.add(v);
            }
        }
        return total;
    }

    /** Média entre os pontos que já têm dados. */
    public BigDecimal getMedia() {
        long com = valores.stream().filter(v -> v != null).count();
        return com == 0 ? BigDecimal.ZERO : getTotal().divide(BigDecimal.valueOf(com), 2, RoundingMode.HALF_UP);
    }

    /** Índice do maior valor, ou -1 se não houver dados. */
    public int getIndiceMelhor() {
        int melhor = -1;
        for (int i = 0; i < valores.size(); i++) {
            BigDecimal v = valores.get(i);
            if (v != null && (melhor < 0 || v.compareTo(valores.get(melhor)) > 0)) {
                melhor = i;
            }
        }
        return melhor;
    }

    public boolean temDados() {
        return getIndiceMelhor() >= 0;
    }
}
