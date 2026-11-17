package geeks.model;

public class Dispositivo {
    private final String nome;
    private final TipoDispositivo tipo;

    public Dispositivo(String nome, TipoDispositivo tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public TipoDispositivo getTipo() {
        return tipo;
    }

    @Override
    public String toString() {
        return nome;
    }
}
