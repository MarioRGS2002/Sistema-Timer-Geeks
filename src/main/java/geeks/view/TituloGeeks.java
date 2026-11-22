package geeks.view;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.Locale;

/** Título da página sobre uma faixa amarela inclinada (a "pincelada" dos posts). */
public class TituloGeeks extends JComponent {
    private static final int INCLINACAO = 14;
    private final String texto;

    public TituloGeeks(String texto) {
        this.texto = texto.toUpperCase(Locale.ROOT);
        setFont(Tema.fonte(Font.BOLD | Font.ITALIC, 36));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(texto) + 56 + INCLINACAO, fm.getHeight() + 6);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(Tema.AMARELO);
        g2.fill(new Polygon(new int[]{INCLINACAO, w, w - INCLINACAO, 0}, new int[]{0, 0, h, h}, 4));
        g2.setColor(Tema.FUNDO);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(texto, 28 + INCLINACAO / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
    }
}
