package geeks.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

public class Cliente {
    private final int idCliente;
    private final String nome;
    private final LocalDate dataNascimento;
    private final String telefone;
    private final String cpf; // somente dígitos ("" quando não informado)
    private final String responsavel;
    private Plano plano = Plano.NENHUM;

    public Cliente(int idCliente, String nome, LocalDate dataNascimento, String telefone, String cpf, String responsavel) {
        this.idCliente = idCliente;
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.telefone = telefone == null ? "" : telefone;
        this.cpf = cpf == null ? "" : cpf;
        this.responsavel = responsavel == null ? "" : responsavel;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public boolean isMenor() {
        return getIdade() < 18;
    }

    public String getNome() {
        return nome;
    }

    /** Idade calculada a partir de data_nascimento. */
    public int getIdade() {
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getCpf() {
        return cpf;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public Plano getPlano() {
        return plano;
    }

    public void setPlano(Plano plano) {
        this.plano = plano;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Cliente && ((Cliente) o).idCliente == idCliente;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }

    @Override
    public String toString() {
        return nome;
    }
}
