package geeks.model;

import geeks.dados.FuncionarioDAO;
import geeks.dados.Transacao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Gestão de funcionários pelo gerente: cadastrar, trocar senha, desativar e reativar.
 *
 * <p>Regras checadas aqui (back-end), não só na tela:</p>
 * <ul>
 *   <li>só um GERENTE <b>ativo no banco</b> pode usar (conferido de novo a cada ação: um gerente
 *       desativado em outro computador perde o acesso na hora, mesmo com o sistema aberto);</li>
 *   <li>ninguém desativa a si mesmo;</li>
 *   <li>sempre sobra pelo menos um gerente ativo;</li>
 *   <li>senha com pelo menos 8 caracteres, guardada só como hash BCrypt;</li>
 *   <li>nada é apagado: desativar só impede o login (o histórico continua com o nome dele).</li>
 * </ul>
 */
public class FuncionariosModel {
    public static final int SENHA_MINIMA = 8;
    private final FuncionarioDAO dao = new FuncionarioDAO();

    public List<Funcionario> listar() {
        Contexto.exigirGerente();
        return Transacao.consultar(c -> {
            exigirGerenteAtivo(c);
            return dao.listar(c).stream().map(FuncionariosModel::converter).toList();
        });
    }

    /** Cadastra um funcionário novo. A senha é apagada da memória ao final. */
    public Funcionario cadastrar(String login, String nome, Perfil perfil, char[] senha, char[] confirmacao) {
        Contexto.exigirGerente();
        try {
            String l = normalizarLogin(login);
            String n = nome == null ? "" : nome.trim();
            validar(l, n, perfil == null ? "" : perfil.name());
            validarSenha(senha, confirmacao);
            String hash = LoginModel.gerarHash(senha);
            return Transacao.executar(c -> {
                exigirGerenteAtivo(c);
                if (dao.existeLogin(c, l)) {
                    throw new IllegalArgumentException("Já existe um funcionário com o login \"" + l + "\" (pode estar desativado: reative-o).");
                }
                int id = dao.inserir(c, l, n, perfil.name(), hash);
                return new Funcionario(id, l, n, perfil, true);
            });
        } finally {
            limpar(senha, confirmacao);
        }
    }

    /** Define uma senha nova (ex.: funcionário esqueceu). Vale também para o próprio gerente. */
    public void trocarSenha(int idFuncionario, char[] senha, char[] confirmacao) {
        Contexto.exigirGerente();
        try {
            validarSenha(senha, confirmacao);
            String hash = LoginModel.gerarHash(senha);
            Transacao.executar(c -> {
                exigirGerenteAtivo(c);
                dao.travar(c, idFuncionario).orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));
                dao.definirSenha(c, idFuncionario, hash);
                return null;
            });
        } finally {
            limpar(senha, confirmacao);
        }
    }

    public void desativar(int idFuncionario) {
        Contexto.exigirGerente();
        if (idFuncionario == Contexto.idFuncionario()) {
            throw new IllegalArgumentException("Você não pode desativar o seu próprio acesso.");
        }
        Transacao.executar(c -> {
            // Trava TODOS os gerentes ativos antes de qualquer outra linha: dois gerentes desativando um ao
            // outro ao mesmo tempo esperam na mesma ordem (sem deadlock) e nunca sobra zero gerente.
            int gerentes = dao.gerentesAtivosTravando(c);
            exigirGerenteAtivo(c);
            FuncionarioDAO.Linha f = dao.travar(c, idFuncionario)
                    .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));
            if (!f.ativo) {
                return null;
            }
            if ("GERENTE".equals(f.cargo) && gerentes <= 1) {
                throw new IllegalArgumentException("Este é o único gerente ativo. Cadastre outro gerente antes de desativá-lo.");
            }
            dao.definirAtivo(c, idFuncionario, false);
            return null;
        });
    }

    public void reativar(int idFuncionario) {
        Contexto.exigirGerente();
        Transacao.executar(c -> {
            exigirGerenteAtivo(c);
            dao.travar(c, idFuncionario).orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));
            dao.definirAtivo(c, idFuncionario, true);
            return null;
        });
    }

    // ---------------------------------------------------------------- regras compartilhadas

    /** Login: 3 a 50 caracteres, letras minúsculas, números, ponto, _ ou -. */
    public static String normalizarLogin(String login) {
        return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
    }

    public static void validar(String login, String nome, String cargo) {
        if (!login.matches("[a-z0-9._-]{3,50}")) {
            throw new IllegalArgumentException("Login inválido: use de 3 a 50 letras minúsculas, números, ponto, _ ou - (sem espaços e sem acento).");
        }
        if (nome == null || nome.isBlank() || nome.trim().length() > 100) {
            throw new IllegalArgumentException("Informe o nome (até 100 caracteres).");
        }
        if (!cargo.equals("ATENDENTE") && !cargo.equals("GERENTE")) {
            throw new IllegalArgumentException("Cargo deve ser ATENDENTE ou GERENTE.");
        }
    }

    public static void validarSenha(char[] senha, char[] confirmacao) {
        if (senha == null || senha.length < SENHA_MINIMA) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos " + SENHA_MINIMA + " caracteres.");
        }
        if (!Arrays.equals(senha, confirmacao)) {
            throw new IllegalArgumentException("As senhas não conferem.");
        }
    }

    /** O gerente logado ainda está ativo e é gerente NO BANCO (não só no login desta execução). */
    private void exigirGerenteAtivo(Connection c) throws SQLException {
        FuncionarioDAO.Linha eu = dao.travar(c, Contexto.idFuncionario()).orElse(null);
        if (eu == null || !eu.ativo || !"GERENTE".equals(eu.cargo)) {
            throw new SecurityException("Seu acesso de gerente foi desativado. Feche o sistema e entre de novo.");
        }
    }

    private static Funcionario converter(FuncionarioDAO.Linha l) {
        return new Funcionario(l.id, l.login, l.nome, Perfil.valueOf(l.cargo), l.ativo);
    }

    private static void limpar(char[]... senhas) {
        for (char[] s : senhas) {
            if (s != null) {
                Arrays.fill(s, '\0');
            }
        }
    }
}
