package geeks.view;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Painel de cantos arredondados usado como "cartão" em todas as telas. */
public class CartaoGeeks extends JPanel {
    private final Color fundo;
    private final Color borda;
    private final boolean tracejado;

    public CartaoGeeks() {
        this(Tema.CARTAO, Tema.BORDA, false);
    }

    public CartaoGeeks(Color fundo, Color borda, boolean tracejado) {
        this.fundo = fundo;
        this.borda = borda;
        this.tracejado = tracejado;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!tracejado) {
            g2.setColor(fundo);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
        }
        g2.setColor(borda);
        g2.setStroke(tracejado
                ? new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 6f}, 0f)
                : new BasicStroke(1f));
        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 28, 28);
        g2.dispose();
    }
}
