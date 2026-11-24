package geeks.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Janela principal: menu lateral com o logo da Geeks e as telas do sistema. */
public class JanelaPrincipal extends JFrame {
    public static final String SESSOES = "SESSOES";
    public static final String RESERVAS = "RESERVAS";
    public static final String HISTORICO = "HISTORICO";
    public static final String CLUBE = "CLUBE";
    public static final String CADASTROS = "CADASTROS";
    public static final String FATURAMENTO = "FATURAMENTO";
    public static final String FUNCIONARIOS = "FUNCIONARIOS";

    private final CardLayout cartas = new CardLayout();
    private final JPanel conteudo = new JPanel(cartas);
    private final Map<String, BotaoNavegacao> botoes = new LinkedHashMap<>();
    private final ButtonGroup grupo = new ButtonGroup();
    private final Map<String, PainelPagina> paineis = new HashMap<>();
    private MenuLateral menu;
    private final boolean gerente;
    private PainelFaturamento painelFaturamento;
    private PainelFuncionarios painelFuncionarios;
    private final JLabel lbUsuario = Componentes.rotulo(" ", Font.PLAIN, 14, Tema.BRANCO);
    private final BotaoGeeks btSair = new BotaoGeeks("Sair", Icone.Tipo.SETA_ESQUERDA, BotaoGeeks.Estilo.CONTORNO);
    private Timer timerRelogio;

    private final PainelSessoes painelSessoes = new PainelSessoes();
    private final PainelReservas painelReservas = new PainelReservas();
    private final PainelHistorico painelHistorico = new PainelHistorico();
    private final PainelClube painelClube = new PainelClube();
    private final PainelCadastro painelCadastro = new PainelCadastro();

    public JanelaPrincipal() throws HeadlessException {
        this(false);
    }

    /** @param gerente true libera as telas exclusivas do gerente (Faturamento e Funcionários) */
    public JanelaPrincipal(boolean gerente) throws HeadlessException {
        this.gerente = gerente;
        setTitle("Geeks - Sistema de Gestão");
        setLayout(new BorderLayout());
        add(criarMenuLateral(), BorderLayout.WEST);

        registrar(SESSOES, painelSessoes);
        registrar(RESERVAS, painelReservas);
        registrar(HISTORICO, painelHistorico);
        registrar(CLUBE, painelClube);
        registrar(CADASTROS, painelCadastro);
        if (gerente) {
            painelFaturamento = new PainelFaturamento();
            registrar(FATURAMENTO, painelFaturamento);
            painelFuncionarios = new PainelFuncionarios();
            registrar(FUNCIONARIOS, painelFuncionarios);
        }
        add(conteudo, BorderLayout.CENTER);

        List<Image> icones = Tema.iconesDaJanela();
        if (!icones.isEmpty()) {
            setIconImages(icones);
        }
        setSize(1280, 720);
        setMinimumSize(new Dimension(1100, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void registrar(String chave, PainelPagina painel) {
        conteudo.add(painel, chave);
        paineis.put(chave, painel);
    }

    private JPanel criarMenuLateral() {
        menu = new MenuLateral();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Tema.LATERAL);
        menu.setPreferredSize(new Dimension(236, 0));
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDA),
                BorderFactory.createEmptyBorder(28, 20, 28, 20)));

        ImageIcon logo = Tema.imagem("/img/geeks_logo.png", 176);
        JLabel lbLogo = logo != null ? new JLabel(logo) : Componentes.rotulo("geeks", Font.BOLD, 40, Tema.BRANCO);
        lbLogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(lbLogo);
        menu.add(Box.createVerticalStrut(30));

        adicionarItem(menu, SESSOES, "Sessões", Icone.Tipo.CONTROLE);
        adicionarItem(menu, RESERVAS, "Reservas", Icone.Tipo.CALENDARIO);
        adicionarItem(menu, HISTORICO, "Histórico", Icone.Tipo.RELOGIO);
        adicionarItem(menu, CLUBE, "Clube Geeks", Icone.Tipo.COROA);
        adicionarItem(menu, CADASTROS, "Cadastros", Icone.Tipo.USUARIO_MAIS);
        if (gerente) {
            adicionarItem(menu, FATURAMENTO, "Faturamento", Icone.Tipo.MOEDA);
            adicionarItem(menu, FUNCIONARIOS, "Funcionários", Icone.Tipo.CADEADO);
        }
        menu.add(Box.createVerticalGlue());
        menu.add(criarUsuarioESair());
        menu.add(Box.createVerticalStrut(12));
        menu.add(criarRelogio());
        return menu;
    }

    /** Quem está logado e o botão Sair (troca de atendente para gerente sem fechar o sistema). */
    private JPanel criarUsuarioESair() {
        JPanel p = Componentes.transparente(new BorderLayout(0, 8));
        p.add(lbUsuario, BorderLayout.NORTH);
        p.add(btSair, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    /** Mostra no menu quem está usando o sistema (ex.: "Maria · Atendente"). */
    public void setUsuario(String nome, String perfil) {
        lbUsuario.setText("<html><b>" + nome + "</b><br><span style='color:#B8B8B8'>" + perfil + "</span></html>");
    }

    public void addListenerSair(ActionListener l) {
        btSair.addActionListener(l);
    }

    @Override
    public void dispose() {
        if (timerRelogio != null) {
            timerRelogio.stop();
        }
        super.dispose();
    }

    /** Relógio no rodapé do menu: o atendente vê a hora sem sair do sistema. */
    private JPanel criarRelogio() {
        JLabel hora = Componentes.rotulo("", Font.BOLD, 30, Tema.BRANCO);
        JLabel data = Componentes.rotulo("", Font.PLAIN, 14, Tema.CINZA);
        Runnable atualizar = () -> {
            LocalDateTime agora = LocalDateTime.now();
            hora.setText(agora.format(DateTimeFormatter.ofPattern("HH:mm")));
            String d = agora.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMM", new Locale("pt", "BR")));
            data.setText(Character.toUpperCase(d.charAt(0)) + d.substring(1));
        };
        atualizar.run();
        timerRelogio = new Timer(1_000, e -> atualizar.run());
        timerRelogio.start();

        JPanel caixa = new JPanel(new GridLayout(2, 1, 0, 2)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0x1E1E1E));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g.setColor(Tema.AMARELO);
                g.fillRoundRect(0, 12, 4, getHeight() - 24, 4, 4);
                g.dispose();
            }
        };
        caixa.setOpaque(false);
        caixa.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 14));
        caixa.add(hora);
        caixa.add(data);
        caixa.setAlignmentX(Component.LEFT_ALIGNMENT);
        caixa.setMaximumSize(new Dimension(Integer.MAX_VALUE, caixa.getPreferredSize().height));
        return caixa;
    }

    private void adicionarItem(JPanel menu, String chave, String texto, Icone.Tipo icone) {
        BotaoNavegacao b = new BotaoNavegacao(texto, icone);
        b.setActionCommand(chave);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
        botoes.put(chave, b);
        grupo.add(b);
        menu.add(b);
        menu.add(Box.createVerticalStrut(6));
    }

    public void mostrarTela(String chave) {
        if (!paineis.containsKey(chave)) {
            return; // tela inexistente ou sem permissão (ex.: Faturamento para quem não é gerente)
        }
        cartas.show(conteudo, chave);
        botoes.get(chave).setSelected(true);
        menu.mover(botoes.get(chave));
        paineis.get(chave).animarEntrada();
    }

    /** O actionCommand do evento é a chave da tela (SESSOES, RESERVAS, ...). */
    public void addListenerNavegacao(ActionListener listener) {
        botoes.values().forEach(b -> b.addActionListener(listener));
    }

    public PainelSessoes getPainelSessoes() {
        return painelSessoes;
    }

    public PainelReservas getPainelReservas() {
        return painelReservas;
    }

    public PainelHistorico getPainelHistorico() {
        return painelHistorico;
    }

    public PainelClube getPainelClube() {
        return painelClube;
    }

    public boolean isGerente() {
        return gerente;
    }

    /** Só existe quando o usuário logado é gerente (senão devolve null). */
    public PainelFaturamento getPainelFaturamento() {
        return painelFaturamento;
    }

    /** Só existe quando o usuário logado é gerente (senão devolve null). */
    public PainelFuncionarios getPainelFuncionarios() {
        return painelFuncionarios;
    }

    public PainelCadastro getPainelCadastro() {
        return painelCadastro;
    }
}
