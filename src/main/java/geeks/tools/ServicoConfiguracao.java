package geeks.tools;

import geeks.dados.Banco;
import geeks.dados.ConfigBanco;
import geeks.dados.FuncionarioDAO;
import geeks.dados.Sql;
import geeks.dados.Transacao;
import geeks.model.FuncionariosModel;
import geeks.model.LoginModel;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Tudo o que a primeira configuração da loja faz, sem tela nem console: usado pelo assistente com
 * telas ({@link AssistenteConfiguracao}) e pelo modo texto ({@link ConfigurarLoja} --console).
 *
 * <p>Segurança: a senha do root do MySQL só é usada para criar bancos e usuários e não é guardada;
 * as senhas internas (geeks_migrator e geeks_app) são aleatórias e ficam em
 * %ProgramData%\SistemaGeeks\db.properties, legível só pelo usuário do Windows; a senha do gerente
 * vira hash BCrypt.</p>
 */
public class ServicoConfiguracao {
    static final String URL_BANCO = "jdbc:mysql://localhost:3306/geeks?useUnicode=true&characterEncoding=utf8"
            + "&connectionTimeZone=-03:00&forceConnectionTimeZoneToSession=true";
    private static final String URL_ROOT = "jdbc:mysql://localhost:3306/?allowMultiQueries=true";

    private final Path pasta = ConfigBanco.pastaInstalacao();
    private final Path arquivoConfig = pasta.resolve("db.properties");
    private Properties config;

    public Path getArquivoConfig() {
        return arquivoConfig;
    }

    /** Este computador já tem configuração (de uma instalação anterior)? */
    public boolean jaConfigurado() {
        return Files.isRegularFile(arquivoConfig);
    }

    /**
     * Já configurado e o banco responde com as senhas guardadas? Nesse caso não precisa pedir a
     * senha do root de novo (ex.: abrir o assistente só para mexer no backup).
     */
    public boolean bancoPronto() {
        if (!jaConfigurado()) {
            return false;
        }
        try {
            Properties p = carregar();
            try (Connection c = DriverManager.getConnection(p.getProperty("db.url"), p.getProperty("db.migrator.usuario"),
                    p.getProperty("db.migrator.senha"))) {
                return c.isValid(3);
            }
        } catch (IOException | SQLException e) {
            return false;
        }
    }

    /** Configuração existente ou uma nova com senhas internas aleatórias. */
    public Properties carregar() throws IOException {
        if (config != null) {
            return config;
        }
        Properties p = new Properties();
        if (jaConfigurado()) {
            try (var r = Files.newBufferedReader(arquivoConfig, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        } else {
            p.setProperty("db.url", URL_BANCO);
            p.setProperty("db.migrator.usuario", "geeks_migrator");
            p.setProperty("db.migrator.senha", senhaAleatoria(24));
            p.setProperty("db.app.usuario", "geeks_app");
            p.setProperty("db.app.senha", senhaAleatoria(24));
            p.setProperty("db.pool.minimo", "2");
            p.setProperty("db.pool.maximo", "5");
        }
        config = p;
        return p;
    }

    /**
     * Passo 1: com a senha do root, cria os bancos e os usuários do sistema, grava a configuração
     * e cria as tabelas (migrations). A senha do root é apagada da memória no final.
     * @throws IllegalArgumentException senha errada ou MySQL fora do ar (mensagem para a tela)
     */
    public void prepararBanco(String usuarioRoot, char[] senhaRoot) throws IOException, SQLException {
        Properties p = carregar();
        String sql = "SET @senha_migrator = " + textoSql(p.getProperty("db.migrator.senha")) + ";\n"
                + "SET @senha_app = " + textoSql(p.getProperty("db.app.senha")) + ";\n"
                + Files.readString(arquivoDoPrograma("db/scripts/01_criar_bancos_e_usuarios.sql"), StandardCharsets.UTF_8)
                .replace("\uFEFF", "");
        String usuario = usuarioRoot == null || usuarioRoot.isBlank() ? "root" : usuarioRoot.trim();
        try (Connection c = conectarRoot(usuario, senhaRoot); Statement st = c.createStatement()) {
            boolean temResultado = st.execute(sql);
            while (temResultado || st.getUpdateCount() != -1) {
                temResultado = st.getMoreResults();
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1045) {
                throw new IllegalArgumentException("Senha do MySQL incorreta. É a senha do usuário \"" + usuario
                        + "\" definida quando o MySQL Server foi instalado.");
            }
            if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
                throw new IllegalArgumentException("Não encontrei o MySQL neste computador. Confira se o MySQL Server "
                        + "está instalado e ligado (procure \"Serviços\" no menu Iniciar e veja o serviço MySQL).");
            }
            throw e;
        } finally {
            if (senhaRoot != null) {
                Arrays.fill(senhaRoot, '\0');
            }
        }
        Files.createDirectories(pasta);
        gravar(p);
        iniciarBanco();
    }

    /** Liga o sistema ao banco (migrations + pool). */
    public void iniciarBanco() {
        Banco.iniciar(ConfigBanco.deArquivo(arquivoConfig));
    }

    // ---------------------------------------------------------------- gerente

    /** Login do primeiro gerente ativo, ou null se ainda não existe nenhum. */
    public String gerenteExistente() {
        return Transacao.consultar(c -> {
            try (var ps = Sql.preparar(c, "SELECT f.nome FROM funcionario AS f WHERE f.cargo = 'GERENTE' AND f.ativo = 1 "
                    + "ORDER BY f.id_funcionario LIMIT 1"); ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        });
    }

    /** Cadastra o gerente (mesmas regras da tela Funcionários). As senhas são apagadas da memória. */
    public void cadastrarGerente(String login, String nome, char[] senha, char[] confirmacao) {
        try {
            String l = FuncionariosModel.normalizarLogin(login);
            String n = nome == null ? "" : nome.trim();
            FuncionariosModel.validar(l, n, "GERENTE");
            FuncionariosModel.validarSenha(senha, confirmacao);
            String hash = LoginModel.gerarHash(senha);
            Transacao.executar(c -> new FuncionarioDAO().criarOuAtualizar(c, l, n, "GERENTE", hash));
        } finally {
            if (senha != null) {
                Arrays.fill(senha, '\0');
            }
            if (confirmacao != null) {
                Arrays.fill(confirmacao, '\0');
            }
        }
    }

    // ---------------------------------------------------------------- equipamentos

    public List<String> tiposDeEquipamento() {
        return Transacao.consultar(c -> {
            List<String> l = new ArrayList<>();
            try (var ps = Sql.preparar(c, "SELECT t.nome FROM tipo_equipamento AS t ORDER BY t.id_tipo");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(rs.getString(1));
                }
            }
            return l;
        });
    }

    /** Equipamentos já cadastrados, como "PC01 (PC)". */
    public List<String> equipamentos() {
        return Transacao.consultar(c -> {
            List<String> l = new ArrayList<>();
            try (var ps = Sql.preparar(c, "SELECT e.identificador, t.nome FROM equipamento AS e "
                    + "INNER JOIN tipo_equipamento AS t ON t.id_tipo = e.id_tipo ORDER BY e.identificador");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(rs.getString(1) + " (" + rs.getString(2) + ")");
                }
            }
            return l;
        });
    }

    public static void validarEquipamento(String nome) {
        String n = nome == null ? "" : nome.trim();
        if (n.isEmpty() || n.length() > 50) {
            throw new IllegalArgumentException("Dê um nome de 1 a 50 caracteres ao equipamento (ex.: PC01).");
        }
    }

    /** Cadastra vários equipamentos {nome, tipo} de uma vez; ignora nomes que já existem. Devolve quantos entraram. */
    public int cadastrarEquipamentos(List<String[]> novos) {
        return Transacao.executar(c -> {
            int n = 0;
            for (String[] e : novos) {
                validarEquipamento(e[0]);
                n += Sql.atualizar(c, "INSERT INTO equipamento (identificador, id_tipo, data_aquisicao, status) "
                        + "SELECT ?, t.id_tipo, CURDATE(), 'DISPONIVEL' FROM tipo_equipamento AS t "
                        + "WHERE t.nome = ? AND NOT EXISTS (SELECT 1 FROM equipamento AS q WHERE q.identificador = ?)",
                        e[0].trim(), e[1], e[0].trim());
            }
            return n;
        });
    }

    // ---------------------------------------------------------------- backup

    public static boolean seteZipInstalado() {
        return Files.isRegularFile(Paths.get("C:\\Program Files\\7-Zip\\7z.exe"))
                || Files.isRegularFile(Paths.get("C:\\Program Files (x86)\\7-Zip\\7z.exe"));
    }

    /**
     * Prepara as senhas do backup, agenda o backup diário e faz o primeiro backup agora.
     * @return a senha que abre os arquivos de backup (o usuário precisa guardá-la)
     */
    public String configurarBackup(String destino, String hora) throws IOException, InterruptedException {
        if (!hora.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            throw new IllegalArgumentException("Horário inválido.");
        }
        Path scripts = arquivoDoPrograma("db/backup/backup.ps1").getParent();
        powershell(scripts.resolve("configurar_backup.ps1"));
        powershell(scripts.resolve("agendar_backup.ps1"), "-Hora", hora, "-Destino", destino);
        powershell(scripts.resolve("backup.ps1"), "-Destino", destino);
        return senhaDoBackup();
    }

    /** Lê a senha dos backups (null se ainda não existe). */
    public String senhaDoBackup() throws IOException {
        for (Path p : List.of(pasta.resolve("senha-backup.txt"), arquivoDoPrograma("db/backup").resolve("senha-backup.txt"))) {
            if (Files.isRegularFile(p)) {
                return Files.readString(p, StandardCharsets.UTF_8).replace("\uFEFF", "").trim();
            }
        }
        return null;
    }

    private static void powershell(Path script, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden", "-File", script.toString()));
        cmd.addAll(Arrays.asList(args));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String saida = new String(p.getInputStream().readAllBytes(), java.nio.charset.Charset.defaultCharset());
        if (p.waitFor() != 0) {
            String[] linhas = saida.strip().split("\\R");
            String ultima = linhas.length == 0 ? "" : linhas[linhas.length - 1];
            throw new IllegalStateException("O backup não pôde ser configurado (" + script.getFileName() + "): " + ultima);
        }
    }

    // ---------------------------------------------------------------- utilidades

    private Connection conectarRoot(String usuario, char[] senha) throws SQLException {
        String s = senha == null ? "" : new String(senha);
        try {
            return DriverManager.getConnection(URL_ROOT, usuario, s);
        } catch (SQLException e) {
            // MySQL sem SSL + caching_sha2_password. Conexão local (localhost), só nesta etapa.
            if (e.getMessage() != null && e.getMessage().contains("Public Key Retrieval")) {
                return DriverManager.getConnection(URL_ROOT + "&allowPublicKeyRetrieval=true", usuario, s);
            }
            throw e;
        }
    }

    private void gravar(Properties p) throws IOException {
        StringBuilder sb = new StringBuilder("# Gerado pelo Configurar Sistema Geeks. NÃO compartilhe este arquivo.\n");
        for (String k : List.of("db.url", "db.migrator.usuario", "db.migrator.senha", "db.app.usuario", "db.app.senha",
                "db.pool.minimo", "db.pool.maximo")) {
            sb.append(k).append('=').append(p.getProperty(k)).append('\n');
        }
        Files.writeString(arquivoConfig, sb.toString(), StandardCharsets.UTF_8);
        restringirAcesso(arquivoConfig);
    }

    /** Só o usuário atual do Windows, o SYSTEM e os Administradores leem o arquivo. */
    private static void restringirAcesso(Path arquivo) {
        String usuario = System.getenv("USERNAME");
        if (usuario == null || usuario.isBlank()) {
            return;
        }
        try {
            Process pr = new ProcessBuilder("icacls", arquivo.toString(), "/inheritance:r", "/grant:r",
                    usuario + ":(R,W)", "*S-1-5-18:(F)", "*S-1-5-32-544:(F)").redirectErrorStream(true).start();
            pr.getInputStream().readAllBytes();
            pr.waitFor();
        } catch (IOException e) {
            // sem icacls: o arquivo continua na pasta do ProgramData (permissões padrão)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Arquivo instalado junto com o programa (pasta do .jar) ou, em desenvolvimento, na pasta do projeto. */
    static Path arquivoDoPrograma(String relativo) {
        List<Path> bases = new ArrayList<>();
        try {
            Path jar = Paths.get(ServicoConfiguracao.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path p = Files.isRegularFile(jar) ? jar.getParent() : jar;
            bases.add(p);
            if (p.getParent() != null) {
                bases.add(p.getParent());
                if (p.getParent().getParent() != null) {
                    bases.add(p.getParent().getParent()); // target/classes -> raiz do projeto
                }
            }
        } catch (URISyntaxException | SecurityException | NullPointerException ignorado) {
            // segue para a pasta atual
        }
        bases.add(Paths.get("").toAbsolutePath());
        for (Path b : bases) {
            Path f = b.resolve(relativo);
            if (Files.exists(f)) {
                return f;
            }
        }
        throw new IllegalStateException("Arquivo do programa não encontrado: " + relativo + ". Reinstale o Sistema Geeks.");
    }

    static String senhaAleatoria(int tamanho) {
        String letras = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tamanho; i++) {
            sb.append(letras.charAt(r.nextInt(letras.length())));
        }
        return sb.toString();
    }

    private static String textoSql(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'";
    }

    static String minusculo(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
