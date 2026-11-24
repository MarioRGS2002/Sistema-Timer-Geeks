package geeks.tools;

import geeks.view.BotaoGeeks;
import geeks.view.CaixaMarcacao;
import geeks.view.CampoGeeks;
import geeks.view.CartaoGeeks;
import geeks.view.Componentes;
import geeks.view.Icone;
import geeks.view.SeletorHorario;
import geeks.view.SeletorOpcoes;
import geeks.view.SenhaGeeks;
import geeks.view.Tema;
import geeks.view.TituloGeeks;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Assistente de primeira configuração COM TELAS (substitui a janela de texto do PowerShell).
 * Abre sozinho quando o sistema não está configurado neste computador, e pelo atalho
 * "Configurar Sistema Geeks". Passos: boas-vindas, banco (senha do root do MySQL), gerente,
 * equipamentos, backup e pronto.
 */
public class AssistenteConfiguracao extends JFrame {
    private static final String[] PASSOS = {"Início", "Banco de dados", "Gerente", "Equipamentos", "Backup", "Pronto"};

    private final ServicoConfiguracao servico;
    private final Runnable aoConcluir;
    private final CardLayout cartas = new CardLayout();
    private final JPanel paginas = Componentes.transparente(cartas);
    private final JLabel lbPasso = Componentes.rotulo(" ", Font.BOLD, 14, Tema.AMARELO);
    private final JLabel lbMensagem = Componentes.rotulo(" ", Font.PLAIN, 14, Tema.CINZA);
    private final BotaoGeeks btVoltar = new BotaoGeeks("Voltar", Icone.Tipo.SETA_ESQUERDA, BotaoGeeks.Estilo.CONTORNO);
    private final BotaoGeeks btAvancar = new BotaoGeeks("Começar", Icone.Tipo.SETA_DIREITA, BotaoGeeks.Estilo.PREENCHIDO);
    private int passo;
    private boolean ocupado;
    private boolean concluido;

    // passo 1
    private final SenhaGeeks campoSenhaRoot = new SenhaGeeks("Senha do MySQL");
    private final JPanel areaBanco = Componentes.transparente(new BorderLayout(0, 14));
    // passo 2
    private final CampoGeeks campoNome = new CampoGeeks("Nome completo");
    private final CampoGeeks campoLogin = new CampoGeeks("ex.: maria");
    private final SenhaGeeks campoSenha = new SenhaGeeks("Mínimo 8 caracteres");
    private final SenhaGeeks campoConfirma = new SenhaGeeks("Repita a senha");
    private final JLabel lbGerenteExiste = Componentes.rotulo(" ", Font.PLAIN, 15, Tema.CINZA);
    private String gerente;
    // passo 3
    private final CampoGeeks campoEquip = new CampoGeeks("Nome que aparece nas telas (ex.: PC01)");
    private SeletorOpcoes tipos;
    private final JPanel areaTipos = Componentes.transparente(new BorderLayout());
    private final JPanel listaEquip = Componentes.transparente(new GridLayout(0, 2, 8, 8));
    private final List<String[]> novosEquip = new ArrayList<>();
    private List<String> equipExistentes = new ArrayList<>();
    // passo 4
    private final CaixaMarcacao chkBackup = new CaixaMarcacao("Fazer backup automático todo dia", true);
    private final CampoGeeks campoPasta = new CampoGeeks("Pasta do backup");
    private final SeletorHorario horaBackup = new SeletorHorario(LocalTime.of(23, 30));
    private final JPanel areaBackup = Componentes.transparente(new BorderLayout(0, 14));
    // passo 5
    private final JPanel areaPronto = Componentes.transparente(new BorderLayout(0, 14));
    private String senhaBackup;

    /** @param aoConcluir o que fazer ao clicar "Abrir o Sistema Geeks" no fim (ex.: abrir o login) */
    public AssistenteConfiguracao(ServicoConfiguracao servico, Runnable aoConcluir) {
        super("Configurar Sistema Geeks");
        this.servico = servico;
        this.aoConcluir = aoConcluir;
        List<java.awt.Image> icones = Tema.iconesDaJanela();
        if (!icones.isEmpty()) {
            setIconImages(icones);
        }

        JPanel fundo = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Tema.FUNDO);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        fundo.setBorder(new EmptyBorder(26, 34, 22, 34));

        JPanel topo = Componentes.transparente(new BorderLayout(0, 6));
        ImageIcon logo = Tema.imagem("/img/geeks_logo.png", 150);
        topo.add(logo != null ? new JLabel(logo) : Componentes.rotulo("geeks", Font.BOLD, 34, Tema.BRANCO), BorderLayout.WEST);
        topo.add(lbPasso, BorderLayout.EAST);
        topo.setBorder(new EmptyBorder(0, 0, 18, 0));

        paginas.add(paginaInicio(), "0");
        paginas.add(paginaBanco(), "1");
        paginas.add(paginaGerente(), "2");
        paginas.add(paginaEquipamentos(), "3");
        paginas.add(paginaBackup(), "4");
        paginas.add(paginaPronto(), "5");

        JPanel botoes = Componentes.transparente(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botoes.add(btVoltar);
        botoes.add(btAvancar);
        JPanel rodape = Componentes.transparente(new BorderLayout(16, 0));
        rodape.setBorder(new EmptyBorder(16, 0, 0, 0));
        rodape.add(lbMensagem, BorderLayout.CENTER);
        rodape.add(botoes, BorderLayout.EAST);

        fundo.add(topo, BorderLayout.NORTH);
        fundo.add(paginas, BorderLayout.CENTER);
        fundo.add(rodape, BorderLayout.SOUTH);
        setContentPane(fundo);

        btVoltar.addActionListener(e -> irPara(passo - 1));
        btAvancar.addActionListener(e -> avancar());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fechar();
            }
        });
        setSize(880, 660);
        setMinimumSize(new Dimension(720, 560));
        setLocationRelativeTo(null);
        irPara(0);
    }

    // ---------------------------------------------------------------- páginas

    private JPanel pagina(String titulo, String texto, JComponent conteudo) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 14));
        JPanel cab = Componentes.transparente(new BorderLayout(0, 10));
        cab.add(new TituloGeeks(titulo), BorderLayout.NORTH);
        if (texto != null) {
            cab.add(Componentes.rotulo("<html><body style='width:560px'>" + texto + "</body></html>", Font.PLAIN, 16,
                    Tema.CINZA), BorderLayout.CENTER);
        }
        p.add(cab, BorderLayout.NORTH);
        if (conteudo != null) {
            JPanel envolve = Componentes.transparente(new BorderLayout());
            envolve.add(conteudo, BorderLayout.NORTH);
            p.add(envolve, BorderLayout.CENTER);
        }
        return p;
    }

    private JPanel comRotulo(String rotulo, JComponent campo) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 6));
        p.add(Componentes.rotulo(rotulo, Font.BOLD, 14, Tema.BRANCO), BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    private JPanel paginaInicio() {
        JPanel itens = Componentes.transparente(new GridLayout(0, 1, 0, 10));
        itens.add(Componentes.rotuloIcone("A senha do MySQL (a que você criou ao instalar o MySQL Server)", Icone.Tipo.CADEADO, Tema.BRANCO, 16));
        itens.add(Componentes.rotuloIcone("O nome e a senha do gerente", Icone.Tipo.COROA, Tema.BRANCO, 16));
        itens.add(Componentes.rotuloIcone("Os nomes dos computadores e consoles da loja", Icone.Tipo.CONTROLE, Tema.BRANCO, 16));
        itens.add(Componentes.rotuloIcone("Onde guardar o backup (de preferência um pendrive ou HD externo)", Icone.Tipo.ESTRELA, Tema.BRANCO, 16));
        return pagina("Bem-vindo!", "Vamos deixar o Sistema Geeks pronto para usar neste computador. Leva uns 5 minutos. "
                + "Tenha em mãos:", itens);
    }

    private JPanel paginaBanco() {
        campoSenhaRoot.setIconeEsquerda(Icone.Tipo.CADEADO);
        campoSenhaRoot.addActionListener(e -> avancar());
        return pagina("Banco de dados", "O Sistema Geeks guarda tudo no MySQL deste computador. Para preparar o banco, "
                + "digite a senha do MySQL. Ela foi criada quando o MySQL Server foi instalado. Ela é usada só agora e "
                + "não fica salva.", areaBanco);
    }

    private void montarBanco() {
        areaBanco.removeAll();
        if (servico.bancoPronto()) {
            areaBanco.add(Componentes.rotuloIcone("O banco deste computador já está configurado. Pode continuar.",
                    Icone.Tipo.CHECK, new Color(0x3DDC84), 17), BorderLayout.NORTH);
        } else {
            JPanel linha = Componentes.transparente(new BorderLayout());
            linha.add(comRotulo("Senha do MySQL", campoSenhaRoot), BorderLayout.CENTER);
            linha.setPreferredSize(new Dimension(420, 70));
            JPanel esquerda = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
            esquerda.add(linha);
            areaBanco.add(esquerda, BorderLayout.NORTH);
            areaBanco.add(Componentes.rotulo("<html><body style='width:560px'>Não sabe a senha? Ela foi definida na instalação "
                    + "do MySQL. Se ninguém lembra, dá para redefinir (veja o README, seção \"Problemas comuns\").</body></html>",
                    Font.PLAIN, 13, new Color(0x8A8A8A)), BorderLayout.CENTER);
        }
        areaBanco.revalidate();
        areaBanco.repaint();
    }

    private JPanel paginaGerente() {
        campoSenha.setIconeEsquerda(Icone.Tipo.CADEADO);
        campoConfirma.setIconeEsquerda(Icone.Tipo.CADEADO);
        JPanel campos = Componentes.transparente(new GridLayout(0, 1, 0, 12));
        campos.add(lbGerenteExiste);
        campos.add(comRotulo("Nome do gerente", campoNome));
        JPanel l2 = Componentes.transparente(new GridLayout(1, 1));
        l2.add(comRotulo("Login (usado para entrar no sistema)", campoLogin));
        campos.add(l2);
        JPanel senhas = Componentes.transparente(new GridLayout(1, 2, 16, 0));
        senhas.add(comRotulo("Senha", campoSenha));
        senhas.add(comRotulo("Repita a senha", campoConfirma));
        campos.add(senhas);
        return pagina("Gerente", "O gerente entra no sistema, vê o faturamento e o caixa, e cadastra os outros "
                + "funcionários na tela Funcionários.", campos);
    }

    private JPanel paginaEquipamentos() {
        JPanel cadastro = Componentes.transparente(new BorderLayout(0, 12));
        JPanel linha = Componentes.transparente(new BorderLayout(10, 0));
        BotaoGeeks adicionar = new BotaoGeeks("Adicionar", Icone.Tipo.MAIS, BotaoGeeks.Estilo.ESCURO);
        adicionar.addActionListener(e -> adicionarEquipamento());
        campoEquip.addActionListener(e -> adicionarEquipamento());
        linha.add(comRotulo("Nome do equipamento", campoEquip), BorderLayout.CENTER);
        JPanel btLinha = Componentes.transparente(new BorderLayout());
        btLinha.add(adicionar, BorderLayout.SOUTH);
        linha.add(btLinha, BorderLayout.EAST);
        cadastro.add(linha, BorderLayout.NORTH);
        cadastro.add(comRotulo("Tipo", areaTipos), BorderLayout.CENTER);
        JPanel tudo = Componentes.transparente(new BorderLayout(0, 16));
        tudo.add(cadastro, BorderLayout.NORTH);
        tudo.add(listaEquip, BorderLayout.CENTER);
        return pagina("Equipamentos", "Cadastre cada computador e console da loja com o nome que vai aparecer nas telas "
                + "(PC01, PC02, Ps5...). Depois dá para acrescentar outros abrindo este assistente de novo.", tudo);
    }

    private void montarEquipamentos() {
        if (tipos == null) {
            tipos = new SeletorOpcoes(servico.tiposDeEquipamento(), 0);
            areaTipos.add(tipos, BorderLayout.WEST);
        }
        equipExistentes = servico.equipamentos();
        listaEquip.removeAll();
        for (String e : equipExistentes) {
            listaEquip.add(itemEquipamento(e + "  (já cadastrado)", null));
        }
        for (String[] e : novosEquip) {
            listaEquip.add(itemEquipamento(e[0] + " (" + e[1] + ")", e));
        }
        if (equipExistentes.isEmpty() && novosEquip.isEmpty()) {
            listaEquip.add(Componentes.rotulo("Nenhum equipamento ainda.", Font.PLAIN, 15, new Color(0x8A8A8A)));
        }
        listaEquip.revalidate();
        listaEquip.repaint();
    }

    private JComponent itemEquipamento(String texto, String[] novo) {
        CartaoGeeks c = new CartaoGeeks();
        c.setLayout(new BorderLayout(8, 0));
        c.setBorder(new EmptyBorder(8, 14, 8, 8));
        c.add(Componentes.rotuloIcone(texto, Icone.Tipo.CONTROLE, novo == null ? Tema.CINZA : Tema.BRANCO, 15), BorderLayout.CENTER);
        if (novo != null) {
            BotaoGeeks remover = new BotaoGeeks("Remover", null, BotaoGeeks.Estilo.LINK);
            remover.addActionListener(e -> {
                novosEquip.remove(novo);
                montarEquipamentos();
            });
            c.add(remover, BorderLayout.EAST);
        }
        return c;
    }

    private void adicionarEquipamento() {
        String nome = campoEquip.getText().trim();
        try {
            ServicoConfiguracao.validarEquipamento(nome);
        } catch (IllegalArgumentException e) {
            erro(e.getMessage());
            return;
        }
        boolean repetido = novosEquip.stream().anyMatch(x -> x[0].equalsIgnoreCase(nome))
                || equipExistentes.stream().anyMatch(x -> x.toLowerCase().startsWith(nome.toLowerCase() + " ("));
        if (repetido) {
            erro("Já existe um equipamento chamado " + nome + ".");
            return;
        }
        novosEquip.add(new String[]{nome, tipos.getSelecionada()});
        campoEquip.setText("");
        campoEquip.requestFocusInWindow();
        info(" ");
        montarEquipamentos();
    }

    private JPanel paginaBackup() {
        return pagina("Backup", "O backup é uma cópia de segurança de tudo (clientes, sessões, pagamentos), feita todo dia "
                + "e protegida por senha. Se o computador estragar, os dados não se perdem.", areaBackup);
    }

    private void montarBackup() {
        areaBackup.removeAll();
        if (!ServicoConfiguracao.seteZipInstalado()) {
            areaBackup.add(Componentes.rotulo("<html><body style='width:560px'><b>O 7-Zip não está instalado</b>, e ele é "
                    + "necessário para o backup. Instale-o pelo site 7-zip.org e abra este assistente de novo pelo menu "
                    + "Iniciar (\"Configurar Sistema Geeks\"). Por enquanto, pode continuar sem backup.</body></html>",
                    Font.PLAIN, 16, new Color(0xFFB800)), BorderLayout.NORTH);
            chkBackup.setSelected(false);
        } else {
            if (campoPasta.getText().isBlank()) {
                campoPasta.setText("C:\\GeeksBackup");
            }
            chkBackup.setSelected(true);
            JPanel pasta = Componentes.transparente(new BorderLayout(10, 0));
            pasta.add(campoPasta, BorderLayout.CENTER);
            BotaoGeeks escolher = new BotaoGeeks("Escolher pasta", Icone.Tipo.LISTA, BotaoGeeks.Estilo.CONTORNO);
            escolher.addActionListener(e -> escolherPasta());
            // backup desmarcado: pasta e horário ficam desativados
            for (java.awt.event.ActionListener l : chkBackup.getActionListeners()) {
                chkBackup.removeActionListener(l);
            }
            chkBackup.addActionListener(e -> {
                boolean on = chkBackup.isSelected();
                campoPasta.setEnabled(on);
                escolher.setEnabled(on);
                horaBackup.setEnabled(on);
                for (java.awt.Component comp : horaBackup.getComponents()) {
                    comp.setEnabled(on);
                }
            });
            pasta.add(escolher, BorderLayout.EAST);
            JPanel campos = Componentes.transparente(new GridLayout(0, 1, 0, 14));
            JPanel chk = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
            chk.add(chkBackup);
            campos.add(chk);
            campos.add(comRotulo("Onde guardar (de preferência um pendrive ou HD externo)", pasta));
            JPanel hora = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
            hora.add(horaBackup);
            campos.add(comRotulo("Horário (o computador precisa estar ligado; se estiver desligado, o backup é feito "
                    + "quando ligar)", hora));
            areaBackup.add(campos, BorderLayout.NORTH);
        }
        areaBackup.revalidate();
        areaBackup.repaint();
    }

    private void escolherPasta() {
        // a janela de escolher pasta com a aparência do Windows (mais familiar), e depois volta ao tema do sistema
        javax.swing.LookAndFeel tema = javax.swing.UIManager.getLookAndFeel();
        JFileChooser fc;
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            fc = new JFileChooser(campoPasta.getText());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            fc = new JFileChooser(campoPasta.getText());
        } finally {
            try {
                javax.swing.UIManager.setLookAndFeel(tema);
            } catch (javax.swing.UnsupportedLookAndFeelException ignorado) {
                // mantém o tema atual
            }
        }
        fc.setDialogTitle("Onde guardar o backup");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showDialog(this, "Usar esta pasta") == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            campoPasta.setText(f.getAbsolutePath());
        }
    }

    private JPanel paginaPronto() {
        return pagina("Tudo pronto!", null, areaPronto);
    }

    private void montarPronto() {
        areaPronto.removeAll();
        JPanel p = Componentes.transparente(new GridLayout(0, 1, 0, 12));
        p.add(Componentes.rotuloIcone("O Sistema Geeks está configurado. Entre com o login do gerente.", Icone.Tipo.CHECK,
                new Color(0x3DDC84), 17));
        if (senhaBackup != null) {
            p.add(Componentes.rotulo("<html><body style='width:560px'><b>Guarde a senha do backup</b> num lugar seguro "
                    + "(anote no papel e guarde, ou num gerenciador de senhas). Sem ela não dá para abrir os backups:</body></html>",
                    Font.PLAIN, 16, Tema.BRANCO));
            JPanel linha = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 12, 0));
            CartaoGeeks caixaSenha = new CartaoGeeks(Tema.CAMPO, Tema.AMARELO, false);
            caixaSenha.setBorder(new EmptyBorder(10, 16, 10, 16));
            JLabel lb = new JLabel(senhaBackup);
            lb.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
            lb.setForeground(Tema.AMARELO);
            caixaSenha.add(lb);
            linha.add(caixaSenha);
            BotaoGeeks copiar = new BotaoGeeks("Copiar senha", Icone.Tipo.LISTA, BotaoGeeks.Estilo.CONTORNO);
            copiar.addActionListener(e -> {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(senhaBackup), null);
                info("Senha copiada. Cole num lugar seguro.");
            });
            linha.add(copiar);
            p.add(linha);
        }
        p.add(Componentes.rotulo("<html><body style='width:560px'>Para acrescentar equipamentos ou mudar o backup depois, "
                + "abra \"Configurar Sistema Geeks\" no menu Iniciar.</body></html>", Font.PLAIN, 14, Tema.CINZA));
        areaPronto.add(p, BorderLayout.NORTH);
        areaPronto.revalidate();
        areaPronto.repaint();
    }

    // ---------------------------------------------------------------- navegação

    private void irPara(int novo) {
        passo = Math.max(0, Math.min(novo, PASSOS.length - 1));
        info(" ");
        switch (passo) {
            case 1:
                montarBanco();
                break;
            case 2:
                gerente = servico.gerenteExistente();
                lbGerenteExiste.setText(gerente == null ? "Cadastre o gerente da loja."
                        : "Já existe o gerente " + gerente + ". Pode continuar ou cadastrar outro.");
                break;
            case 3:
                montarEquipamentos();
                break;
            case 4:
                montarBackup();
                break;
            case 5:
                montarPronto();
                break;
            default:
                break;
        }
        cartas.show(paginas, String.valueOf(passo));
        lbPasso.setText(passo == 0 || passo == 5 ? " " : "Passo " + passo + " de 4  ·  " + PASSOS[passo]);
        btVoltar.setVisible(passo > 1 && passo < 5);
        btAvancar.setText(passo == 0 ? "Começar" : passo == 5 ? "Abrir o Sistema Geeks" : "Continuar");
        btAvancar.setEnabled(true);
        if (passo == 1 && campoSenhaRoot.isShowing()) {
            campoSenhaRoot.requestFocusInWindow();
        }
    }

    private void avancar() {
        if (ocupado) {
            return;
        }
        switch (passo) {
            case 0:
                irPara(1);
                break;
            case 1:
                if (servico.bancoPronto()) {
                    trabalhar("Ligando ao banco...", () -> {
                        servico.iniciarBanco();
                        return null;
                    }, r -> irPara(2));
                } else {
                    char[] senha = campoSenhaRoot.getPassword();
                    if (senha.length == 0) {
                        erro("Digite a senha do MySQL.");
                        return;
                    }
                    trabalhar("Preparando o banco de dados (pode levar alguns segundos)...", () -> {
                        servico.prepararBanco("root", senha);
                        return null;
                    }, r -> {
                        campoSenhaRoot.setText("");
                        irPara(2);
                    });
                }
                break;
            case 2:
                boolean preencheu = !campoNome.getText().isBlank() || !campoLogin.getText().isBlank()
                        || campoSenha.getPassword().length > 0;
                if (gerente != null && !preencheu) {
                    irPara(3);
                    return;
                }
                char[] s = campoSenha.getPassword();
                char[] c = campoConfirma.getPassword();
                String login = campoLogin.getText();
                String nome = campoNome.getText();
                trabalhar("Salvando o gerente...", () -> {
                    servico.cadastrarGerente(login, nome, s, c);
                    return null;
                }, r -> {
                    campoSenha.setText("");
                    campoConfirma.setText("");
                    irPara(3);
                });
                break;
            case 3:
                if (equipExistentes.isEmpty() && novosEquip.isEmpty()) {
                    erro("Cadastre pelo menos um equipamento (digite o nome, escolha o tipo e clique em Adicionar).");
                    return;
                }
                List<String[]> lista = new ArrayList<>(novosEquip);
                trabalhar("Salvando os equipamentos...", () -> servico.cadastrarEquipamentos(lista), r -> {
                    novosEquip.clear();
                    irPara(4);
                });
                break;
            case 4:
                if (!chkBackup.isSelected() || !ServicoConfiguracao.seteZipInstalado()) {
                    try {
                        senhaBackup = servico.senhaDoBackup();
                    } catch (java.io.IOException e) {
                        senhaBackup = null;
                    }
                    irPara(5);
                    return;
                }
                String destino = campoPasta.getText().trim();
                if (destino.isEmpty()) {
                    erro("Escolha a pasta do backup.");
                    return;
                }
                String hora = horaBackup.getHora().format(DateTimeFormatter.ofPattern("HH:mm"));
                trabalhar("Agendando o backup e fazendo o primeiro backup agora...",
                        () -> servico.configurarBackup(destino, hora), r -> {
                            senhaBackup = r;
                            irPara(5);
                        });
                break;
            default:
                concluido = true;
                dispose();
                if (aoConcluir != null) {
                    aoConcluir.run();
                }
                break;
        }
    }

    /** Executa fora da tela (sem travar a janela), mostra "aguarde" e, no fim, o erro ou o próximo passo. */
    private <T> void trabalhar(String aguarde, Callable<T> tarefa, java.util.function.Consumer<T> depois) {
        ocupado = true;
        btAvancar.setEnabled(false);
        btVoltar.setEnabled(false);
        info(aguarde);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarefa.call();
            }

            @Override
            protected void done() {
                ocupado = false;
                btAvancar.setEnabled(true);
                btVoltar.setEnabled(true);
                try {
                    depois.accept(get());
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable causa = e.getCause();
                    erro(causa.getMessage() == null ? "Erro inesperado: " + causa : causa.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void erro(String msg) {
        lbMensagem.setForeground(Tema.VERMELHO);
        lbMensagem.setText("<html><body style='width:430px'>" + msg.replace("<", "&lt;") + "</body></html>");
    }

    private void info(String msg) {
        lbMensagem.setForeground(Tema.CINZA);
        lbMensagem.setText(msg);
    }

    private void fechar() {
        if (ocupado) {
            return; // espera a etapa terminar
        }
        if (!concluido && passo < 5 && !geeks.view.Dialogos.confirmar(this, "Fechar a configuração",
                "A configuração não terminou. Fechar mesmo assim? Dá para continuar depois abrindo o Sistema Geeks.",
                "Fechar", true)) {
            return;
        }
        dispose();
        System.exit(0);
    }
}
