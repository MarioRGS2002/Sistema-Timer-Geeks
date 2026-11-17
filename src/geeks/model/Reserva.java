package geeks.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Reserva {
    private static int contador = 0;

    private final String id;
    private final Cliente cliente;
    private final Dispositivo dispositivo;
    private LocalDateTime dataHora;

    public Reserva(Cliente cliente, Dispositivo dispositivo, LocalDateTime dataHora) {
        this.id = "R" + (++contador);
        this.cliente = cliente;
        this.dispositivo = dispositivo;
        this.dataHora = dataHora;
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

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    @Override
    public String toString() {
        return dataHora.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + dispositivo.getNome()
                + " - " + cliente.getNome();
    }
}
