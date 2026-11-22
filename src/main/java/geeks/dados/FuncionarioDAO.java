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

    /** Linha da lista de funcionários (tela Funcionários, só gerente). Sem o hash da senha. */
    public static final class Linha {
        public final int id;
        public final String login;
        public final String nome;
        public final String cargo;
        public final boolean ativo;

        Linha(int id, String login, String nome, String cargo, boolean ativo) {
            this.id = id;
            this.login = login;
            this.nome = nome;
            this.cargo = cargo;
            this.ativo = ativo;
        }
    }

    /** Todos os funcionários: ativos primeiro, gerentes antes de atendentes, depois por nome. */
    public java.util.List<Linha> listar(Connection c) throws SQLException {
        String sql = "SELECT f.id_funcionario, f.login, f.nome, f.cargo, f.ativo FROM funcionario AS f "
                + "ORDER BY f.ativo DESC, f.cargo DESC, f.nome";
        java.util.List<Linha> l = new java.util.ArrayList<>();
        try (PreparedStatement ps = Sql.preparar(c, sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                l.add(lerLinha(rs));
            }
        }
        return l;
    }

    /** Trava a linha do funcionário (SELECT ... FOR UPDATE) e devolve os dados; vazio se não existir. */
    public Optional<Linha> travar(Connection c, int id) throws SQLException {
        String sql = "SELECT f.id_funcionario, f.login, f.nome, f.cargo, f.ativo FROM funcionario AS f "
                + "WHERE f.id_funcionario = ? FOR UPDATE";
        try (PreparedStatement ps = Sql.preparar(c, sql, id); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(lerLinha(rs)) : Optional.empty();
        }
    }

    /** Trava e conta os gerentes ativos (serializa duas desativações de gerente ao mesmo tempo). */
    public int gerentesAtivosTravando(Connection c) throws SQLException {
        String sql = "SELECT f.id_funcionario FROM funcionario AS f WHERE f.cargo = 'GERENTE' AND f.ativo = 1 FOR UPDATE";
        int n = 0;
        try (PreparedStatement ps = Sql.preparar(c, sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                n++;
            }
        }
        return n;
    }

    public boolean existeLogin(Connection c, String login) throws SQLException {
        return Sql.contar(c, "SELECT COUNT(*) FROM funcionario WHERE login = ?", login) > 0;
    }

    /** Cadastra um funcionário novo (login repetido é recusado pela chave única do banco). */
    public int inserir(Connection c, String login, String nome, String cargo, String senhaHash) throws SQLException {
        return Sql.inserir(c, "INSERT INTO funcionario (nome, login, senha_hash, cargo, data_admissao) "
                + "VALUES (?, ?, ?, ?, CURDATE())", nome, login, senhaHash, cargo);
    }

    public void definirSenha(Connection c, int id, String senhaHash) throws SQLException {
        Sql.atualizar(c, "UPDATE funcionario SET senha_hash = ? WHERE id_funcionario = ?", senhaHash, id);
    }

    /** Desativar não apaga: o funcionário continua no histórico (sessões e pagamentos), só não entra mais. */
    public void definirAtivo(Connection c, int id, boolean ativo) throws SQLException {
        Sql.atualizar(c, "UPDATE funcionario SET ativo = ? WHERE id_funcionario = ?", ativo ? 1 : 0, id);
    }

    private static Linha lerLinha(ResultSet rs) throws SQLException {
        return new Linha(rs.getInt("id_funcionario"), rs.getString("login"), rs.getString("nome"),
                rs.getString("cargo"), rs.getInt("ativo") == 1);
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
