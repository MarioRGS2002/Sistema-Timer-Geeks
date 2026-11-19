package geeks.view;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Base de todas as telas: fundo preto, mancha amarela no canto, título e área de conteúdo. */
public abstract class PainelPagina extends JPanel {
    private final JPanel cabecalho = Componentes.transparente(new BorderLayout());
    private final JPanel corpo = Componentes.transparente(new BorderLayout());
    private float entrada = 1f;

    protected PainelPagina(String titulo) {
        super(new BorderLayout());
        setBorder(new EmptyBorder(28, 40, 28, 40));
        cabecalho.setBorder(new EmptyBorder(0, 0, 22, 0));
        cabecalho.add(new TituloGeeks(titulo), BorderLayout.WEST);
        add(cabecalho, BorderLayout.NORTH);
        add(corpo, BorderLayout.CENTER);
    }

    /** Ao abrir a tela, o conteúdo sobe suavemente enquanto aparece. */
    public void animarEntrada() {
        entrada = 0f;
        Animacao.executar(280, 0, p -> {
            entrada = (float) Animacao.suave(p);
            repaint();
        }, () -> {
            entrada = 1f;
            repaint();
        });
    }

    @Override
    protected void paintChildren(Graphics g0) {
        if (entrada >= 0.999f) {
            super.paintChildren(g0);
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setComposite(AlphaComposite.SrcOver.derive(entrada));
        g.translate(0, (1 - entrada) * 16);
        super.paintChildren(g);
        g.dispose();
    }

    protected void setLadoDireito(JComponent componente) {
        cabecalho.add(componente, BorderLayout.EAST);
    }

    protected JPanel getCorpo() {
        return corpo;
    }

    protected JScrollPane rolagem(JComponent conteudo) {
        JScrollPane scroll = new JScrollPane(conteudo, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        ScrollGeeks.aplicar(scroll.getVerticalScrollBar());
        return scroll;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g.setColor(Tema.FUNDO);
        g.fillRect(0, 0, w, h);
        g.setColor(Tema.AMARELO);
        g.fillOval(w - 150, h - 160, 260, 260);
        g.dispose();
    }
}
