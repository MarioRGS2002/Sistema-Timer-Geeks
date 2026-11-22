package geeks.dados;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Acesso à tabela funcionario. Senhas só como hash BCrypt. */
public class FuncionarioDAO {

    /** Dados mínimos para autenticar. */
    public static final class Registro {
        public final int id;
        public final String login;
        public final String nome;
        public final String senhaHash;
        public final String cargo;

        Registro(int id, String login, String nome, String senhaHash, String cargo) {
            this.id = id;
            this.login = login;
            this.nome = nome;
            this.senhaHash = senhaHash;
            this.cargo = cargo;
        }
    }

    /** Busca funcionário ATIVO pelo login (funcionário inativo não entra). */
    public Optional<Registro> buscarAtivo(Connection c, String login) throws SQLException {
        String sql = "SELECT f.id_funcionario, f.login, f.nome, f.senha_hash, f.cargo "
                + "FROM funcionario AS f WHERE f.login = ? AND f.ativo = 1";
        try (PreparedStatement ps = Sql.preparar(c, sql, login); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new Registro(rs.getInt("id_funcionario"), rs.getString("login"), rs.getString("nome"),
                    rs.getString("senha_hash"), rs.getString("cargo")));
        }
    }

    /** Cria o funcionário ou, se o login já existir, atualiza nome, cargo, senha e reativa. */
    public int criarOuAtualizar(Connection c, String login, String nome, String cargo, String senhaHash) throws SQLException {
        long existe = Sql.contar(c, "SELECT COUNT(*) FROM funcionario WHERE login = ?", login);
        if (existe > 0) {
            Sql.atualizar(c, "UPDATE funcionario SET nome = ?, cargo = ?, senha_hash = ?, ativo = 1 WHERE login = ?",
                    nome, cargo, senhaHash, login);
            return (int) Sql.contar(c, "SELECT id_funcionario FROM funcionario WHERE login = ?", login);
        }
        return Sql.inserir(c, "INSERT INTO funcionario (nome, login, senha_hash, cargo, data_admissao) "
                + "VALUES (?, ?, ?, ?, CURDATE())", nome, login, senhaHash, cargo);
    }
}
