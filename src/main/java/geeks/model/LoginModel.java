package geeks.model;

import geeks.dados.FuncionarioDAO;
import geeks.dados.Transacao;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Autenticação dos funcionários pela tabela funcionario (senha_hash em BCrypt).
 * Funcionário inativo (ativo = 0) não entra. Ao autenticar, o funcionário fica no Contexto.
 */
public class LoginModel {
    /** Hash fixo usado quando o login não existe, para o tempo de resposta não revelar isso. */
    private static final String HASH_FALSO = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BpEwEfI8m6EqTxl0d3HjS4FfwQ8m";

    private final FuncionarioDAO dao = new FuncionarioDAO();

    /**
     * @return o usuário autenticado, ou null se usuário/senha estiverem incorretos
     * @throws IllegalArgumentException se algum campo estiver vazio
     */
    public Usuario autenticar(String login, char[] senha) {
        if (login == null || login.isBlank() || senha == null || senha.length == 0) {
            throw new IllegalArgumentException("Informe usuário e senha.");
        }
        String l = login.trim().toLowerCase(Locale.ROOT);
        Optional<FuncionarioDAO.Registro> reg = Transacao.consultar(c -> dao.buscarAtivo(c, l));
        String texto = new String(senha);
        try {
            boolean confere = BCrypt.checkpw(texto, reg.map(r -> r.senhaHash).orElse(HASH_FALSO));
            if (reg.isEmpty() || !confere) {
                return null;
            }
            FuncionarioDAO.Registro r = reg.get();
            Usuario u = new Usuario(r.id, r.login, r.nome, Perfil.valueOf(r.cargo));
            Contexto.entrar(u);
            return u;
        } catch (IllegalArgumentException e) {
            return null; // hash gravado em formato inválido
        }
    }

    /** Gera o hash BCrypt (custo 11) de uma senha. */
    public static String gerarHash(char[] senha) {
        byte[] bytes = StandardCharsets.UTF_8.encode(CharBuffer.wrap(senha)).array();
        try {
            return BCrypt.hashpw(new String(senha), BCrypt.gensalt(11));
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
