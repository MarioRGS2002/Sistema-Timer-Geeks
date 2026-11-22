package geeks.model;

/** Funcionário que usa o sistema (tabela funcionario). */
public class Usuario {
    private final int idFuncionario;
    private final String login;
    private final String nome;
    private final Perfil perfil;

    public Usuario(int idFuncionario, String login, String nome, Perfil perfil) {
        this.idFuncionario = idFuncionario;
        this.login = login;
        this.nome = nome;
        this.perfil = perfil;
    }

    public int getIdFuncionario() {
        return idFuncionario;
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
}
