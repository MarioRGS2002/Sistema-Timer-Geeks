package geeks.tools;

import geeks.dados.Banco;
import geeks.dados.ConfigBanco;
import geeks.dados.FuncionarioDAO;
import geeks.dados.Transacao;
import geeks.model.LoginModel;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Cria ou atualiza um funcionário (login, nome, cargo e senha em BCrypt).
 * Use para criar o PRIMEIRO GERENTE em produção (não existe senha padrão).
 *
 * <pre>
 * java -cp target\sistema-geeks.jar geeks.tools.CriarUsuario
 * java -cp target\sistema-geeks.jar geeks.tools.CriarUsuario login "Nome Completo" GERENTE
 * </pre>
 * A senha é sempre pedida no console, sem eco (System.console().readPassword).
 * Só em ambiente sem console (ex.: dentro da IDE) ela é lida da entrada padrão.
 */
public final class CriarUsuario {
    private CriarUsuario() {
    }

    public static void main(String[] args) throws Exception {
        Console console = System.console();
        BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        String login = args.length > 0 ? args[0] : ler(console, entrada, "Login: ");
        String nome = args.length > 1 ? args[1] : ler(console, entrada, "Nome: ");
        String cargo = (args.length > 2 ? args[2] : ler(console, entrada, "Cargo (ATENDENTE ou GERENTE): "))
                .trim().toUpperCase(Locale.ROOT);
        login = login.trim().toLowerCase(Locale.ROOT);
        validar(login, nome, cargo);

        char[] senha = lerSenha(console, entrada, "Senha: ");
        char[] confirmacao = lerSenha(console, entrada, "Confirme a senha: ");
        try {
            if (!Arrays.equals(senha, confirmacao)) {
                throw new IllegalArgumentException("As senhas não conferem.");
            }
            if (senha.length < 8) {
                throw new IllegalArgumentException("A senha precisa ter pelo menos 8 caracteres.");
            }
            String hash = LoginModel.gerarHash(senha);
            ConfigBanco config = ConfigBanco.carregar();
            Banco.iniciar(config);
            String l = login;
            int id = Transacao.executar(c -> new FuncionarioDAO().criarOuAtualizar(c, l, nome.trim(), cargo, hash));
            System.out.println("Funcionário \"" + l + "\" (" + cargo + ") salvo com id " + id + ".");
        } finally {
            Arrays.fill(senha, '\0');
            Arrays.fill(confirmacao, '\0');
            Banco.fechar();
        }
    }

    static void validar(String login, String nome, String cargo) {
        if (!login.matches("[a-z0-9._-]{3,50}")) {
            throw new IllegalArgumentException("Login inválido: use de 3 a 50 letras minúsculas, números, ponto, _ ou -.");
        }
        if (nome == null || nome.isBlank() || nome.trim().length() > 100) {
            throw new IllegalArgumentException("Informe um nome de até 100 caracteres.");
        }
        if (!cargo.equals("ATENDENTE") && !cargo.equals("GERENTE")) {
            throw new IllegalArgumentException("Cargo deve ser ATENDENTE ou GERENTE.");
        }
    }

    private static String ler(Console console, BufferedReader entrada, String rotulo) throws Exception {
        if (console != null) {
            String s = console.readLine(rotulo);
            return s == null ? "" : s;
        }
        System.out.print(rotulo);
        String s = entrada.readLine();
        return s == null ? "" : s;
    }

    private static char[] lerSenha(Console console, BufferedReader entrada, String rotulo) throws Exception {
        if (console != null) {
            char[] s = console.readPassword(rotulo);
            return s == null ? new char[0] : s;
        }
        System.out.print(rotulo);
        String s = entrada.readLine();
        return s == null ? new char[0] : s.toCharArray();
    }
}
