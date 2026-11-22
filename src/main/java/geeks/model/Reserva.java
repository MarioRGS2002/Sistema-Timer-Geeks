package geeks.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Reserva de equipamento, sempre em tempo FECHADO (tabela reserva). */
public class Reserva {
    private final int idReserva;
    private final Cliente cliente;
    private final Dispositivo dispositivo;
    private final LocalDateTime dataHora;
    private final LocalDateTime fim;

    public Reserva(int idReserva, Cliente cliente, Dispositivo dispositivo, LocalDateTime dataHora, LocalDateTime fim) {
        this.idReserva = idReserva;
        this.cliente = cliente;
        this.dispositivo = dispositivo;
        this.dataHora = dataHora;
        this.fim = fim;
    }

    public String getId() {
        return "R" + idReserva;
    }

    public int getIdReserva() {
        return idReserva;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Dispositivo getDispositivo() {
        return dispositivo;
    }

    /** Início previsto da reserva. */
    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Reserva && ((Reserva) o).idReserva == idReserva;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReserva);
    }

    @Override
    public String toString() {
        return dataHora.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + dispositivo.getNome()
                + " - " + cliente.getNome();
    }
}
