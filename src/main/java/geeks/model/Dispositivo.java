package geeks.model;

import java.util.Objects;

/** Equipamento da loja (tabela equipamento). */
public class Dispositivo {
    private final int idEquipamento;
    private final String nome;
    private final TipoDispositivo tipo;

    public Dispositivo(int idEquipamento, String nome, TipoDispositivo tipo) {
        this.idEquipamento = idEquipamento;
        this.nome = nome;
        this.tipo = tipo;
    }

    public int getIdEquipamento() {
        return idEquipamento;
    }

    public String getNome() {
        return nome;
    }

    public TipoDispositivo getTipo() {
        return tipo;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Dispositivo && ((Dispositivo) o).idEquipamento == idEquipamento;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEquipamento);
    }

    @Override
    public String toString() {
        return nome;
    }
}
