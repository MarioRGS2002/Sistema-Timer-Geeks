package geeks.tools;

import geeks.dados.Banco;
import geeks.dados.ConfigBanco;
import geeks.dados.FuncionarioDAO;
import geeks.dados.Sql;
import geeks.dados.Transacao;
import geeks.model.LoginModel;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
 * Configuração da loja num computador novo, tudo em um lugar ("Configurar Sistema Geeks" no menu Iniciar).
 *
 * <ol>
 *   <li>pede a senha do root do MySQL (só para criar bancos e usuários; não é guardada);</li>
 *   <li>gera senhas aleatórias para geeks_migrator e geeks_app e grava
 *       %ProgramData%\SistemaGeeks\db.properties (legível só pelo usuário do Windows);</li>
 *   <li>cria as tabelas (migrations);</li>
 *   <li>cadastra o gerente (obrigatório na primeira vez) e outros funcionários;</li>
 *   <li>cadastra os equipamentos;</li>
 *   <li>opcional: prepara e agenda o backup diário.</li>
 * </ol>
 * Pode ser rodado de novo: mantém as senhas e os dados que já existem.
 */
public final class ConfigurarLoja {
    private static final String URL_BANCO = "jdbc:mysql://localhost:3306/geeks?useUnicode=true&characterEncoding=utf8"
            + "&connectionTimeZone=-03:00&forceConnectionTimeZoneToSession=true";
    private static final String URL_ROOT = "jdbc:mysql://localhost:3306/?allowMultiQueries=true";

    private final Console console = System.console();
    private final BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private final PrintWriter out = console != null ? console.writer() : new PrintWriter(System.out, true);

    private ConfigurarLoja() {
    }

    public static void main(String[] args) {
        ConfigurarLoja c = new ConfigurarLoja();
        int codigo = 0;
        try {
            c.executar();
        } catch (Exception e) {
            c.out.println();
            c.out.println("ERRO: " + (e.getMessage() == null ? e : e.getMessage()));
            c.out.println("Nada foi perdido. Corrija o problema e abra o configurador de novo.");
            codigo = 1;
        } finally {
            Banco.fechar();
        }
        c.ler("\nAperte Enter para fechar.");
        System.exit(codigo);
    }

    private void executar() throws Exception {
        titulo("CONFIGURAR SISTEMA GEEKS");
        out.println("Este assistente prepara o banco de dados neste computador.");
        out.println("Antes, o MySQL Server precisa estar instalado (e você precisa saber a senha do root dele).");

        Path pasta = ConfigBanco.pastaInstalacao();
        Path arquivoConfig = pasta.resolve("db.properties");
        Properties p = configuracao(arquivoConfig);

        titulo("1/5  Banco de dados");
        criarBancosEUsuarios(p);
        Files.createDirectories(pasta);
        gravar(arquivoConfig, p);
        out.println("Configuração salva em " + arquivoConfig);

        titulo("2/5  Tabelas");
        ConfigBanco config = ConfigBanco.deArquivo(arquivoConfig);
        Banco.iniciar(config);
        out.println("Tabelas criadas/atualizadas.");

        titulo("3/5  Funcionários");
        funcionarios();

        titulo("4/5  Equipamentos");
        equipamentos();

        titulo("5/5  Backup automático");
        backup(pasta);

        titulo("PRONTO");
        out.println("Abra o sistema pelo atalho \"Sistema Geeks\" na área de trabalho ou no menu Iniciar.");
        out.println("Para cadastrar mais funcionários ou equipamentos depois, abra este configurador de novo.");
    }

    // ---------------------------------------------------------------- 1. bancos e usuários

    private Properties configuracao(Path arquivo) throws IOException {
        Properties p = new Properties();
        if (Files.isRegularFile(arquivo)) {
            try (var r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            out.println("\nEste computador já foi configurado antes: as senhas internas serão mantidas.");
            return p;
        }
        p.setProperty("db.url", URL_BANCO);
        p.setProperty("db.migrator.usuario", "geeks_migrator");
        p.setProperty("db.migrator.senha", senhaAleatoria());
        p.setProperty("db.app.usuario", "geeks_app");
        p.setProperty("db.app.senha", senhaAleatoria());
        p.setProperty("db.pool.minimo", "2");
        p.setProperty("db.pool.maximo", "5");
        return p;
    }

    private void criarBancosEUsuarios(Properties p) throws IOException, SQLException {
        String sql = "SET @senha_migrator = " + textoSql(p.getProperty("db.migrator.senha")) + ";\n"
                + "SET @senha_app = " + textoSql(p.getProperty("db.app.senha")) + ";\n"
                + Files.readString(arquivoDoPrograma("db/scripts/01_criar_bancos_e_usuarios.sql"), StandardCharsets.UTF_8)
                .replace("\uFEFF", "");
        for (int tentativa = 1; ; tentativa++) {
            String usuario = ler("Usuário administrador do MySQL [root]: ").trim();
            if (usuario.isEmpty()) {
                usuario = "root";
            }
            char[] senha = lerSenha("Senha do " + usuario + " (não aparece ao digitar): ");
            try (Connection c = conectarRoot(usuario, senha); Statement st = c.createStatement()) {
                boolean temResultado = st.execute(sql);
                while (temResultado || st.getUpdateCount() != -1) {
                    temResultado = st.getMoreResults();
                }
                out.println("Bancos e usuários do MySQL prontos.");
                return;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1045) {
                    if (tentativa < 3) {
                        out.println("Usuário ou senha do MySQL incorretos. Tente de novo.");
                        continue;
                    }
                    throw new IllegalStateException("Usuário ou senha do MySQL incorretos (3 tentativas). "
                            + "A senha do root é a que foi definida na instalação do MySQL Server.");
                }
                if ("08S01".equals(e.getSQLState()) || (e.getSQLState() != null && e.getSQLState().startsWith("08"))) {
                    throw new IllegalStateException("Não consegui conectar no MySQL em localhost:3306. "
                            + "Confira se o MySQL Server está instalado e ligado (serviço MySQL em Serviços do Windows).");
                }
                throw e;
            } finally {
                Arrays.fill(senha, '\0');
            }
        }
    }

    private Connection conectarRoot(String usuario, char[] senha) throws SQLException {
        try {
            return DriverManager.getConnection(URL_ROOT, usuario, new String(senha));
        } catch (SQLException e) {
            // MySQL sem SSL + caching_sha2_password. Conexão local (localhost), só nesta etapa.
            if (e.getMessage() != null && e.getMessage().contains("Public Key Retrieval")) {
                return DriverManager.getConnection(URL_ROOT + "&allowPublicKeyRetrieval=true", usuario, new String(senha));
            }
            throw e;
        }
    }

    private void gravar(Path arquivo, Properties p) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder("# Gerado pelo Configurar Sistema Geeks. NÃO compartilhe este arquivo.\n");
        for (String k : List.of("db.url", "db.migrator.usuario", "db.migrator.senha", "db.app.usuario", "db.app.senha",
                "db.pool.minimo", "db.pool.maximo")) {
            sb.append(k).append('=').append(p.getProperty(k)).append('\n');
        }
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
        restringirAcesso(arquivo);
    }

    /** Só o usuário atual do Windows, o SYSTEM e os Administradores leem o arquivo. */
    private void restringirAcesso(Path arquivo) throws IOException, InterruptedException {
        String usuario = System.getenv("USERNAME");
        if (usuario == null || usuario.isBlank()) {
            return;
        }
        Process pr = new ProcessBuilder("icacls", arquivo.toString(), "/inheritance:r", "/grant:r",
                usuario + ":(R,W)", "*S-1-5-18:(F)", "*S-1-5-32-544:(F)").redirectErrorStream(true).start();
        pr.getInputStream().readAllBytes();
        if (pr.waitFor() != 0) {
            out.println("Aviso: não consegui restringir o acesso a " + arquivo);
        }
    }

    // ---------------------------------------------------------------- 3. funcionários

    private void funcionarios() throws Exception {
        List<String> existentes = Transacao.consultar(c -> {
            List<String> l = new ArrayList<>();
            try (var ps = Sql.preparar(c, "SELECT f.login, f.cargo FROM funcionario AS f WHERE f.ativo = 1 ORDER BY f.cargo, f.login");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(rs.getString(1) + " (" + rs.getString(2) + ")");
                }
            }
            return l;
        });
        boolean temGerente = existentes.stream().anyMatch(s -> s.endsWith("(GERENTE)"));
        out.println(existentes.isEmpty() ? "Nenhum funcionário cadastrado." : "Já cadastrados: " + String.join(", ", existentes));
        if (!temGerente) {
            out.println("\nCadastre o GERENTE (ele vê o faturamento). Sem ele não dá para entrar no sistema.");
            cadastrarFuncionario("GERENTE");
        }
        while (sim("\nCadastrar " + (temGerente ? "um" : "outro") + " funcionário (atendente)? (s/N): ", false)) {
            cadastrarFuncionario(sim("É gerente? (s/N): ", false) ? "GERENTE" : "ATENDENTE");
            temGerente = true;
        }
    }

    private void cadastrarFuncionario(String cargo) throws Exception {
        while (true) {
            try {
                String login = ler("Login (ex.: maria): ").trim().toLowerCase(Locale.ROOT);
                String nome = ler("Nome completo: ").trim();
                CriarUsuario.validar(login, nome, cargo);
                char[] senha = lerSenha("Senha (mínimo 8 caracteres, não aparece ao digitar): ");
                char[] conf = lerSenha("Repita a senha: ");
                try {
                    if (!Arrays.equals(senha, conf)) {
                        throw new IllegalArgumentException("As senhas não conferem.");
                    }
                    if (senha.length < 8) {
                        throw new IllegalArgumentException("A senha precisa ter pelo menos 8 caracteres.");
                    }
                    String hash = LoginModel.gerarHash(senha);
                    Transacao.executar(c -> new FuncionarioDAO().criarOuAtualizar(c, login, nome, cargo, hash));
                    out.println("Funcionário \"" + login + "\" (" + cargo + ") salvo.");
                    return;
                } finally {
                    Arrays.fill(senha, '\0');
                    Arrays.fill(conf, '\0');
                }
            } catch (IllegalArgumentException e) {
                out.println(e.getMessage() + " Vamos tentar de novo.");
            }
        }
    }

    // ---------------------------------------------------------------- 4. equipamentos

    private void equipamentos() {
        List<String> tipos = Transacao.consultar(c -> {
            List<String> l = new ArrayList<>();
            try (var ps = Sql.preparar(c, "SELECT t.nome FROM tipo_equipamento AS t ORDER BY t.id_tipo");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(rs.getString(1));
                }
            }
            return l;
        });
        List<String> existentes = Transacao.consultar(c -> {
            List<String> l = new ArrayList<>();
            try (var ps = Sql.preparar(c, "SELECT e.identificador, t.nome FROM equipamento AS e "
                    + "JOIN tipo_equipamento AS t ON t.id_tipo = e.id_tipo ORDER BY e.identificador");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(rs.getString(1) + " (" + rs.getString(2) + ")");
                }
            }
            return l;
        });
        out.println(existentes.isEmpty() ? "Nenhum equipamento cadastrado." : "Já cadastrados: " + String.join(", ", existentes));
        out.println("Digite o nome de cada equipamento como deve aparecer nas telas (ex.: PC01, Ps5).");
        while (true) {
            String nome = ler("\nNome do equipamento (Enter vazio para terminar): ").trim();
            if (nome.isEmpty()) {
                return;
            }
            if (nome.length() > 50) {
                out.println("Use no máximo 50 caracteres.");
                continue;
            }
            for (int i = 0; i < tipos.size(); i++) {
                out.println("  " + (i + 1) + ") " + tipos.get(i));
            }
            int n;
            try {
                n = Integer.parseInt(ler("Tipo (número): ").trim());
            } catch (NumberFormatException e) {
                n = 0;
            }
            if (n < 1 || n > tipos.size()) {
                out.println("Tipo inválido.");
                continue;
            }
            String tipo = tipos.get(n - 1);
            int gravados = Transacao.executar(c -> Sql.atualizar(c,
                    "INSERT INTO equipamento (identificador, id_tipo, data_aquisicao, status) "
                            + "SELECT ?, t.id_tipo, CURDATE(), 'DISPONIVEL' FROM tipo_equipamento AS t "
                            + "WHERE t.nome = ? AND NOT EXISTS (SELECT 1 FROM equipamento AS e WHERE e.identificador = ?)",
                    nome, tipo, nome));
            out.println(gravados == 1 ? "Equipamento \"" + nome + "\" (" + tipo + ") cadastrado."
                    : "Já existe um equipamento chamado \"" + nome + "\".");
        }
    }

    // ---------------------------------------------------------------- 5. backup

    private void backup(Path pastaConfig) throws Exception {
        if (!Files.isRegularFile(Paths.get("C:\\Program Files\\7-Zip\\7z.exe"))
                && !Files.isRegularFile(Paths.get("C:\\Program Files (x86)\\7-Zip\\7z.exe"))) {
            out.println("O 7-Zip não está instalado, então o backup automático ficou para depois.");
            out.println("Instale o 7-Zip (site 7-zip.org) e abra este configurador de novo.");
            return;
        }
        if (!sim("Fazer backup automático todo dia? (S/n): ", true)) {
            return;
        }
        String destino = ler("Pasta do backup [C:\\GeeksBackup] (de preferência um HD externo): ").trim();
        if (destino.isEmpty()) {
            destino = "C:\\GeeksBackup";
        }
        String hora = ler("Horário (o computador precisa estar ligado) [23:30]: ").trim();
        if (hora.isEmpty()) {
            hora = "23:30";
        }
        if (!hora.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            out.println("Horário inválido; usando 23:30.");
            hora = "23:30";
        }
        Path pastaScripts = arquivoDoPrograma("db/backup/backup.ps1").getParent();
        powershell(pastaScripts.resolve("configurar_backup.ps1"));
        powershell(pastaScripts.resolve("agendar_backup.ps1"), "-Hora", hora, "-Destino", destino);
        out.println("Fazendo o primeiro backup agora...");
        powershell(pastaScripts.resolve("backup.ps1"), "-Destino", destino);
        out.println("\nIMPORTANTE: a senha que abre os backups está em " + pastaConfig.resolve("senha-backup.txt"));
        out.println("Copie essa senha para um lugar seguro (gerenciador de senhas ou papel guardado). Sem ela o backup não abre.");
    }

    private void powershell(Path script, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString()));
        cmd.addAll(Arrays.asList(args));
        Process p = new ProcessBuilder(cmd).inheritIO().start();
        if (p.waitFor() != 0) {
            throw new IllegalStateException("O script " + script.getFileName() + " terminou com erro (veja as mensagens acima).");
        }
    }

    // ---------------------------------------------------------------- utilidades

    /** Arquivo instalado junto com o programa (pasta do .jar) ou, em desenvolvimento, na pasta do projeto. */
    private static Path arquivoDoPrograma(String relativo) {
        List<Path> bases = new ArrayList<>();
        try {
            Path jar = Paths.get(ConfigurarLoja.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path pasta = Files.isRegularFile(jar) ? jar.getParent() : jar;
            bases.add(pasta);
            if (pasta.getParent() != null) {
                bases.add(pasta.getParent());
            }
        } catch (URISyntaxException | SecurityException | NullPointerException ignorado) {
            // segue para a pasta atual
        }
        bases.add(Paths.get("").toAbsolutePath());
        for (Path b : bases) {
            Path f = b.resolve(relativo);
            if (Files.isRegularFile(f)) {
                return f;
            }
        }
        throw new IllegalStateException("Arquivo do programa não encontrado: " + relativo + ". Reinstale o Sistema Geeks.");
    }

    private static String senhaAleatoria() {
        String letras = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 24; i++) {
            sb.append(letras.charAt(r.nextInt(letras.length())));
        }
        return sb.toString();
    }

    private static String textoSql(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'";
    }

    private void titulo(String t) {
        out.println();
        out.println("==== " + t + " ====");
    }

    private boolean sim(String pergunta, boolean padrao) {
        String r = ler(pergunta).trim().toLowerCase(Locale.ROOT);
        return r.isEmpty() ? padrao : r.startsWith("s");
    }

    private String ler(String rotulo) {
        try {
            if (console != null) {
                String s = console.readLine("%s", rotulo);
                return s == null ? "" : s;
            }
            out.print(rotulo);
            out.flush();
            String s = entrada.readLine();
            return s == null ? "" : s;
        } catch (IOException e) {
            return "";
        }
    }

    private char[] lerSenha(String rotulo) {
        if (console != null) {
            char[] s = console.readPassword("%s", rotulo);
            return s == null ? new char[0] : s;
        }
        return ler(rotulo).toCharArray();
    }
}
