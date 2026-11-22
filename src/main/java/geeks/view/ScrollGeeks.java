package geeks.view;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** Barra de rolagem fina, escura, com "polegar" arredondado (sem setas). */
public class ScrollGeeks extends BasicScrollBarUI {
    public static void aplicar(JScrollBar barra) {
        barra.setUI(new ScrollGeeks());
        barra.setPreferredSize(new Dimension(10, 0));
        barra.setOpaque(false);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return botaoVazio();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return botaoVazio();
    }

    private JButton botaoVazio() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle area) {
        // trilho transparente
    }

    @Override
    protected void paintThumb(Graphics g0, JComponent c, Rectangle area) {
        if (area.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isThumbRollover() || isDragging ? Tema.AMARELO : new Color(0x4A4A4A));
        g.fillRoundRect(area.x + 2, area.y + 2, area.width - 4, area.height - 4, 8, 8);
        g.dispose();
    }
}
