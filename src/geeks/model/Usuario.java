package geeks.model;

public class Usuario {
    private final String login;
    private final String nome;
    private final String hashSenha;
    private final Perfil perfil;

    public Usuario(String login, String nome, String hashSenha, Perfil perfil) {
        this.login = login;
        this.nome = nome;
        this.hashSenha = hashSenha;
        this.perfil = perfil;
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

    String getHashSenha() {
        return hashSenha;
    }
}
