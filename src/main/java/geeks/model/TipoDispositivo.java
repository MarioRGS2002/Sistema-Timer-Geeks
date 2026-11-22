package geeks.model;

import java.math.BigDecimal;

public enum TipoDispositivo {
    // Valores por hora de EXEMPLO: ajustar com o cliente
    PC("PC", "6.00"),
    PLAYSTATION("PlayStation", "12.00"),
    SWITCH("Nintendo Switch", "12.00"),
    XADREZ("Xadrez", "6.00");

    private final String rotulo;
    private final BigDecimal valorHora;

    TipoDispositivo(String rotulo, String valorHora) {
        this.rotulo = rotulo;
        this.valorHora = new BigDecimal(valorHora);
    }

    public String getRotulo() {
        return rotulo;
    }

    public BigDecimal getValorHora() {
        return valorHora;
    }
}
