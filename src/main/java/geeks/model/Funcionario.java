package geeks.model;

/** Funcionário da loja, como aparece na tela Funcionários (só gerente). */
public class Funcionario {
    private final int id;
    private final String login;
    private final String nome;
    private final Perfil perfil;
    private final boolean ativo;

    public Funcionario(int id, String login, String nome, Perfil perfil, boolean ativo) {
        this.id = id;
        this.login = login;
        this.nome = nome;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getNome() {
        return nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isGerente() {
        return perfil == Perfil.GERENTE;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
