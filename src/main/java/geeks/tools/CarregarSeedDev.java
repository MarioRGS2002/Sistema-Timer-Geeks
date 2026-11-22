package geeks.tools;

import geeks.dados.Banco;
import geeks.dados.ConfigBanco;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Carrega os dados de DESENVOLVIMENTO (db/seed-dev/*.sql) no banco do db.properties.
 * Roda as migrations antes e RECUSA se já existirem clientes (para não misturar com dados reais).
 *
 * <pre>java -cp target\sistema-geeks.jar geeks.tools.CarregarSeedDev [arquivo-de-config] [pasta-seed]</pre>
 */
public final class CarregarSeedDev {
    private CarregarSeedDev() {
    }

    public static void main(String[] args) throws Exception {
        ConfigBanco config = args.length > 0 ? ConfigBanco.deArquivo(Paths.get(args[0])) : ConfigBanco.carregar();
        Path pasta = Paths.get(args.length > 1 ? args[1] : "db/seed-dev");
        carregar(config, pasta);
        System.out.println("Dados de desenvolvimento carregados em " + config.nomeDoBanco() + ".");
    }

    public static void carregar(ConfigBanco config, Path pasta) throws Exception {
        Banco.migrar(config);
        try (Connection c = DriverManager.getConnection(config.getUrl(), config.getUsuarioMigrator(), config.getSenhaMigrator());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM cliente")) {
            rs.next();
            if (rs.getLong(1) > 0) {
                throw new IllegalStateException("O banco " + config.nomeDoBanco()
                        + " já tem clientes. O seed de desenvolvimento só roda em banco vazio.");
            }
        }
        try (var arquivos = Files.list(pasta)) {
            for (Path arq : (Iterable<Path>) arquivos.filter(p -> p.toString().endsWith(".sql")).sorted()::iterator) {
                ExecutorSql.executarArquivo(config, arq);
                System.out.println("  executado: " + arq.getFileName());
            }
        }
    }
}
