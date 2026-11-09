package geeks.view;

import geeks.model.PeriodoHistorico;
import geeks.model.Sessao;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PainelHistorico extends PainelPagina {
    private final Map<PeriodoHistorico, BotaoFiltro> filtros = new EnumMap<>(PeriodoHistorico.class);
    private final ButtonGroup grupo = new ButtonGroup();
    private final CampoGeeks campoBusca = new CampoGeeks("Buscar por dispositivo ou cliente");
    private final Componentes.Pilula pilulaTotal = new Componentes.Pilula(Icone.Tipo.TROFEU, "0 sessões");
    private final JPanel lista = Componentes.transparente(new GridLayout(0, 1, 0, 12));
    private ActionListener listenerDados;

    public PainelHistorico() {
        super("Histórico");
        setLadoDireito(pilulaTotal);

        JPanel barra = Componentes.transparente(new BorderLayout());
        barra.setBorder(new EmptyBorder(0, 0, 18, 0));
        JPanel chips = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (PeriodoHistorico p : PeriodoHistorico.values()) {
            BotaoFiltro b = new BotaoFiltro(p.getRotulo());
            b.setActionCommand(p.name());
            filtros.put(p, b);
            grupo.add(b);
            chips.add(b);
        }
        filtros.get(PeriodoHistorico.SEMANA).setSelected(true);
        campoBusca.setIconeEsquerda(Icone.Tipo.LUPA);
        campoBusca.setPreferredSize(new Dimension(330, 44));
        barra.add(chips, BorderLayout.WEST);
        barra.add(campoBusca, BorderLayout.EAST);

        JPanel envoltorio = Componentes.transparente(new BorderLayout());
        envoltorio.add(lista, BorderLayout.NORTH);
        getCorpo().add(barra, BorderLayout.NORTH);
        getCorpo().add(rolagem(envoltorio), BorderLayout.CENTER);
    }

    public void atualizarHistorico(List<Sessao> sessoes) {
        pilulaTotal.setText(sessoes.size() + (sessoes.size() == 1 ? " sessão" : " sessões"));
        lista.removeAll();
        if (sessoes.isEmpty()) {
            lista.add(Componentes.rotulo("Nenhuma sessão encontrada para esse filtro.", Font.PLAIN, 18, Tema.CINZA));
        }
        for (Sessao s : sessoes) {
            lista.add(criarLinha(s));
        }
        lista.revalidate();
        lista.repaint();
    }

    private JComponent criarLinha(Sessao s) {
        CartaoGeeks linha = new CartaoGeeks();
        linha.setLayout(new BorderLayout(16, 0));
        linha.setBorder(new EmptyBorder(12, 18, 12, 18));
        linha.add(new Componentes.BlocoIcone(s.getDispositivo().getTipo(), 48), BorderLayout.WEST);

        JPanel centro = Componentes.transparente(new GridLayout(1, 3, 12, 0));
        JPanel quem = Componentes.transparente(new GridLayout(2, 1, 0, 4));
        quem.add(Componentes.rotulo(s.getCliente().getNome(), Font.BOLD, 18, Tema.BRANCO));
        JPanel linhaChip = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaChip.add(new Componentes.Chip(s.getDispositivo().getNome(), Tema.corDe(s.getDispositivo().getTipo()),
                Tema.iconeDe(s.getDispositivo().getTipo())));
        quem.add(linhaChip);
        JPanel valores = Componentes.transparente(new GridLayout(2, 1, 0, 4));
        valores.add(Componentes.rotuloIcone(Formatador.duracao(s.getDuracao()), Icone.Tipo.RELOGIO, Tema.CINZA, 15));
        valores.add(Componentes.rotuloIcone(Formatador.moeda(s.getValor()), Icone.Tipo.MOEDA, Tema.AMARELO, 15));
        JPanel fim = Componentes.transparente(new GridLayout(2, 1, 0, 4));
        fim.add(Componentes.rotulo("Sessão encerrada em", Font.PLAIN, 14, Tema.CINZA));
        fim.add(Componentes.rotulo(Formatador.dataHora(s.getFim()), Font.BOLD, 16, Tema.BRANCO));
        centro.add(quem);
        centro.add(valores);
        centro.add(fim);
        linha.add(centro, BorderLayout.CENTER);

        BotaoGeeks dados = new BotaoGeeks("Dados do usuário", Icone.Tipo.USUARIO_MAIS, BotaoGeeks.Estilo.LINK);
        dados.setActionCommand(s.getCliente().getCpf());
        if (listenerDados != null) {
            dados.addActionListener(listenerDados);
        }
        linha.add(dados, BorderLayout.EAST);
        return linha;
    }

    public PeriodoHistorico getPeriodoSelecionado() {
        for (Map.Entry<PeriodoHistorico, BotaoFiltro> e : filtros.entrySet()) {
            if (e.getValue().isSelected()) {
                return e.getKey();
            }
        }
        return PeriodoHistorico.SEMANA;
    }

    public String getTextoBusca() {
        return campoBusca.getText();
    }

    public void addListenerFiltro(ActionListener l) {
        filtros.values().forEach(b -> b.addActionListener(l));
    }

    public void addListenerBusca(DocumentListener l) {
        campoBusca.getDocument().addDocumentListener(l);
    }

    /** O actionCommand do evento é o CPF do cliente da linha. */
    public void addListenerDadosUsuario(ActionListener l) {
        this.listenerDados = l;
    }

    private static class BotaoFiltro extends JToggleButton {
        BotaoFiltro(String texto) {
            super(texto);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Tema.fonte(Font.BOLD, 14));
            setBorder(new EmptyBorder(7, 16, 7, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Color getForeground() {
            return isSelected() ? Tema.FUNDO : Tema.BRANCO;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g.setColor(Tema.AMARELO);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            } else {
                g.setColor(new Color(0x444444));
                g.setStroke(new java.awt.BasicStroke(2f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight(), getHeight());
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
