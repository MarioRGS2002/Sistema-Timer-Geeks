package geeks.model;

public class Cliente {
    private final String nome;
    private final int idade;
    private final String telefone;
    private final String cpf; // somente dígitos
    private final String responsavel;
    private Plano plano = Plano.NENHUM;

    public Cliente(String nome, int idade, String telefone, String cpf, String responsavel) {
        this.nome = nome;
        this.idade = idade;
        this.telefone = telefone;
        this.cpf = cpf;
        this.responsavel = responsavel;
    }

    public boolean isMenor() {
        return idade < 18;
    }

    public String getNome() {
        return nome;
    }

    public int getIdade() {
        return idade;
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
    public String toString() {
        return nome;
    }
}
