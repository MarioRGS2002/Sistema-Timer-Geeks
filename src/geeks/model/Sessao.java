package geeks.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public class Sessao {
    private static int contador = 0;

    private final String id;
    private final Cliente cliente;
    private final Dispositivo dispositivo;
    private final LocalDateTime inicio;
    private LocalDateTime fim;

    public Sessao(Cliente cliente, Dispositivo dispositivo, LocalDateTime inicio) {
        this.id = "S" + (++contador);
        this.cliente = cliente;
        this.dispositivo = dispositivo;
        this.inicio = inicio;
    }

    public void encerrar() {
        if (fim == null) {
            fim = LocalDateTime.now();
        }
    }

    void encerrarEm(LocalDateTime momento) {
        this.fim = momento;
    }

    public boolean isAtiva() {
        return fim == null;
    }

    public Duration getDuracao() {
        return Duration.between(inicio, fim != null ? fim : LocalDateTime.now());
    }

    public BigDecimal getValor() {
        BigDecimal horas = BigDecimal.valueOf(getDuracao().getSeconds())
                .divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
        return dispositivo.getTipo().getValorHora().multiply(horas).setScale(2, RoundingMode.HALF_UP);
    }

    public String getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Dispositivo getDispositivo() {
        return dispositivo;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }
}
