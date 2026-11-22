package geeks.model;

/**
 * Funcionário logado nesta execução do sistema. Preenchido pelo LoginModel; usado pelos
 * models para registrar quem abriu sessões e recebeu pagamentos, e para checar o cargo
 * (ex.: faturamento só para GERENTE) no lado do back-end, não só na tela.
 */
public final class Contexto {
    private static volatile Usuario usuario;

    private Contexto() {
    }

    static void entrar(Usuario u) {
        usuario = u;
    }

    public static void sair() {
        usuario = null;
    }

    public static Usuario usuario() {
        Usuario u = usuario;
        if (u == null) {
            throw new SecurityException("Nenhum funcionário está logado.");
        }
        return u;
    }

    public static int idFuncionario() {
        return usuario().getIdFuncionario();
    }

    public static void exigirGerente() {
        if (!usuario().isGerente()) {
            throw new SecurityException("Acesso permitido somente ao gerente.");
        }
    }
}
