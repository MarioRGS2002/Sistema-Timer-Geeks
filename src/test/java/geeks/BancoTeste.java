package geeks;

import com.zaxxer.hikari.HikariDataSource;
import geeks.dados.Banco;
import geeks.dados.ConfigBanco;
import geeks.dados.Sql;
import geeks.model.LoginModel;
import geeks.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Banco de testes geeks_test: DROP, CREATE e migrate a cada teste.
 * RECUSA rodar se o banco configurado não se chamar exatamente geeks_test.
 */
public final class BancoTeste {
    public static final String NOME = "geeks_test";
    private static ConfigBanco config;
    private static final String HASH_SENHA = BCrypt.hashpw("senha-teste-123", BCrypt.gensalt(4));

    private BancoTeste() {
    }

    public static synchronized ConfigBanco config() {
        if (config == null) {
            Path arq = Paths.get("db.test.properties").toAbsolutePath();
            if (!Files.isRegularFile(arq)) {
                throw new IllegalStateException("Crie o db.test.properties (copie de db.test.properties.example).");
            }
            config = ConfigBanco.deArquivo(arq);
        }
        return config;
    }

    /** Apaga e recria o geeks_test, aplica as migrations e liga o pool do geeks_app. */
    public static void resetar() throws SQLException {
        ConfigBanco cfg = config();
        if (!NOME.equals(cfg.nomeDoBanco())) {
            throw new IllegalStateException("Reset recusado: o banco de testes deve se chamar exatamente " + NOME
                    + " (configurado: " + cfg.nomeDoBanco() + ").");
        }
        Banco.fechar();
        try (Connection c = DriverManager.getConnection(cfg.urlSemBanco(), cfg.getUsuarioMigrator(), cfg.getSenhaMigrator());
             Statement st = c.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS " + NOME);
            st.execute("CREATE DATABASE " + NOME + " DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci");
        }
        Banco.migrar(cfg);
        HikariDataSource ds = Banco.criarPool(cfg);
        Banco.usar(ds);
    }

    /** Conexão do usuário da aplicação (geeks_app), autocommit ligado. */
    public static Connection app() throws SQLException {
        return DriverManager.getConnection(config().getUrl(), config().getUsuarioApp(), config().getSenhaApp());
    }

    /** Conexão do migrator (para montar cenários que o app não pode, ex.: conferir permissões). */
    public static Connection migrator() throws SQLException {
        return DriverManager.getConnection(config().getUrl(), config().getUsuarioMigrator(), config().getSenhaMigrator());
    }

    // ---------- montagem de cenários (com o geeks_app, que tem INSERT) ----------

    public static int funcionario(Connection c, String login, String cargo, boolean ativo) throws SQLException {
        return Sql.inserir(c, "INSERT INTO funcionario (nome, login, senha_hash, cargo, ativo) VALUES (?, ?, ?, ?, ?)",
                "Func " + login, login, HASH_SENHA, cargo, ativo ? 1 : 0);
    }

    /** Cria funcionário e faz login (Contexto) com ele. */
    public static Usuario logar(String login, String cargo) throws SQLException {
        try (Connection c = app()) {
            funcionario(c, login, cargo, true);
        }
        return new LoginModel().autenticar(login, "senha-teste-123".toCharArray());
    }

    public static int idTipo(Connection c, String nome) throws SQLException {
        return (int) Sql.contar(c, "SELECT id_tipo FROM tipo_equipamento WHERE nome = ?", nome);
    }

    public static int equipamento(Connection c, String identificador, String tipo) throws SQLException {
        return Sql.inserir(c, "INSERT INTO equipamento (identificador, id_tipo) VALUES (?, ?)", identificador, idTipo(c, tipo));
    }

    public static int cliente(Connection c, String nome, String cpf) throws SQLException {
        return Sql.inserir(c, "INSERT INTO cliente (nome, cpf, data_nascimento) VALUES (?, ?, ?)",
                nome, cpf, LocalDate.now().minusYears(25));
    }

    public static int assinatura(Connection c, int idCliente, LocalDate inicio, LocalDate fim, String situacao) throws SQLException {
        int plano = (int) Sql.contar(c, "SELECT id_plano FROM plano_clube WHERE nome_plano = 'Clube Geeks'");
        return Sql.inserir(c, "INSERT INTO assinatura_clube (data_inicio, data_fim, situacao, id_plano, id_cliente) "
                + "VALUES (?, ?, ?, ?, ?)", inicio, fim, situacao, plano, idCliente);
    }

    /** Sessão em andamento (sem fim) com um participante. */
    public static int sessaoAtiva(Connection c, String tipoTempo, LocalDateTime inicio, LocalDateTime previsto,
                                  int idEquipamento, int idFuncionario, int idCliente) throws SQLException {
        int id = Sql.inserir(c, "INSERT INTO sessao (horario_inicio, horario_previsto, tipo_tempo, valor_hora_contratado, "
                        + "id_equipamento, id_funcionario) VALUES (?, ?, ?, (SELECT t.valor_hora FROM equipamento AS e "
                        + "INNER JOIN tipo_equipamento AS t ON t.id_tipo = e.id_tipo WHERE e.id_equipamento = ?), ?, ?)",
                inicio, previsto, tipoTempo, idEquipamento, idEquipamento, idFuncionario);
        Sql.atualizar(c, "INSERT INTO participante_sessao (id_sessao, id_cliente) VALUES (?, ?)", id, idCliente);
        return id;
    }

    public static BigDecimal decimal(Connection c, String sql, Object... params) throws SQLException {
        try (var ps = Sql.preparar(c, sql, params); var rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : null;
        }
    }
}
