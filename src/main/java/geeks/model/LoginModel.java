package geeks.model;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Regras de autenticação. As senhas ficam guardadas como hash SHA-256 (nunca em texto puro).
 * Os usuários são de EXEMPLO e ficam em memória; no sistema final virão de um banco de dados
 * (com hash que use "sal", como BCrypt).
 */
public class LoginModel {
    private final Map<String, Usuario> usuarios = new HashMap<>();

    public LoginModel() {
        // EXEMPLOS: "atendente" / "geeks123" e "gerente" / "gerente123"
        cadastrar(new Usuario("atendente", "Atendente", hash("geeks123"), Perfil.ATENDENTE));
        cadastrar(new Usuario("gerente", "Gerente", hash("gerente123"), Perfil.GERENTE));
    }

    private void cadastrar(Usuario usuario) {
        usuarios.put(usuario.getLogin(), usuario);
    }

    /**
     * @return o usuário autenticado, ou null se usuário/senha estiverem incorretos
     * @throws IllegalArgumentException se algum campo estiver vazio
     */
    public Usuario autenticar(String login, char[] senha) {
        if (login == null || login.isBlank() || senha == null || senha.length == 0) {
            throw new IllegalArgumentException("Informe usuário e senha.");
        }
        Usuario usuario = usuarios.get(login.trim().toLowerCase(Locale.ROOT));
        String informado = hash(new String(senha));
        if (usuario == null) {
            return null;
        }
        boolean confere = MessageDigest.isEqual(usuario.getHashSenha().getBytes(StandardCharsets.UTF_8),
                informado.getBytes(StandardCharsets.UTF_8));
        return confere ? usuario : null;
    }

    private static String hash(String texto) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
