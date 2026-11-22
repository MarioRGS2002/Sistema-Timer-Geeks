package geeks.model;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Tipos de equipamento conhecidos pela interface (cor e ícone de cada um).
 * O valor por hora vem da tabela tipo_equipamento e é carregado ao iniciar o sistema.
 */
public enum TipoDispositivo {
    PC("PC"),
    PLAYSTATION("PlayStation"),
    SWITCH("Nintendo Switch"),
    XADREZ("Xadrez");

    private final String rotulo;
    private volatile BigDecimal valorHora = BigDecimal.ZERO;
    private volatile int idTipo;

    TipoDispositivo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public BigDecimal getValorHora() {
        return valorHora;
    }

    public int getIdTipo() {
        return idTipo;
    }

    /** Atualiza com os dados da tabela tipo_equipamento. */
    public void definirDoBanco(int idTipo, BigDecimal valorHora) {
        this.idTipo = idTipo;
        this.valorHora = valorHora;
    }

    /** Converte o nome gravado em tipo_equipamento.nome (ex.: "Nintendo Switch"). */
    public static TipoDispositivo doNome(String nome) {
        String n = normalizar(nome);
        for (TipoDispositivo t : values()) {
            if (normalizar(t.rotulo).equals(n) || normalizar(t.name()).equals(n)) {
                return t;
            }
        }
        if (n.contains("play") || n.contains("ps")) {
            return PLAYSTATION;
        }
        if (n.contains("switch") || n.contains("nintendo")) {
            return SWITCH;
        }
        if (n.contains("xadrez")) {
            return XADREZ;
        }
        return PC;
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
    }
}
