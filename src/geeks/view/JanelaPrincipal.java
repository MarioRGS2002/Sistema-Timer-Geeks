package geeks.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Janela principal: menu lateral com o logo da Geeks e as telas do sistema. */
public class JanelaPrincipal extends JFrame {
    public static final String SESSOES = "SESSOES";
    public static final String RESERVAS = "RESERVAS";
    public static final String HISTORICO = "HISTORICO";
    public static final String CLUBE = "CLUBE";
    public static final String CADASTROS = "CADASTROS";

    private final CardLayout cartas = new CardLayout();
    private final JPanel conteudo = new JPanel(cartas);
    private final Map<String, BotaoNavegacao> botoes = new LinkedHashMap<>();
    private final ButtonGroup grupo = new ButtonGroup();
    private final Map<String, PainelPagina> paineis = new HashMap<>();
    private MenuLateral menu;

    private final PainelSessoes painelSessoes = new PainelSessoes();
    private final PainelReservas painelReservas = new PainelReservas();
    private final PainelHistorico painelHistorico = new PainelHistorico();
    private final PainelClube painelClube = new PainelClube();
    private final PainelCadastro painelCadastro = new PainelCadastro();

    public JanelaPrincipal() throws HeadlessException {
        setTitle("Geeks - Sistema de Gestão");
        setLayout(new BorderLayout());
        add(criarMenuLateral(), BorderLayout.WEST);

        registrar(SESSOES, painelSessoes);
        registrar(RESERVAS, painelReservas);
        registrar(HISTORICO, painelHistorico);
        registrar(CLUBE, painelClube);
        registrar(CADASTROS, painelCadastro);
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
        menu.add(Box.createVerticalGlue());
        return menu;
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

    public PainelCadastro getPainelCadastro() {
        return painelCadastro;
    }
}
