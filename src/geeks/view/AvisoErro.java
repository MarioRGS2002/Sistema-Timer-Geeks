package geeks.view;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Linha de aviso (ícone + texto vermelho) que aparece e some com fade, sem mexer no layout. */
public class AvisoErro extends JComponent {
    private String texto = "";
    private float alfa;

    public AvisoErro() {
        setFont(Tema.fonte(Font.BOLD, 14));
    }

    public void mostrar(String mensagem) {
        this.texto = mensagem;
        animar(1f);
    }

    public void ocultar() {
        if (alfa > 0f) {
            animar(0f);
        }
    }

    private void animar(float alvo) {
        final float de = alfa;
        Animacao.executar(220, 0, p -> {
            alfa = (float) (de + (alvo - de) * p);
            repaint();
        }, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100, 26);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (alfa <= 0.01f) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color cor = new Color(Tema.VERMELHO.getRed(), Tema.VERMELHO.getGreen(), Tema.VERMELHO.getBlue(), (int) (255 * alfa));
        new Icone(Icone.Tipo.ALERTA, 18, cor).paintIcon(this, g, 0, (getHeight() - 18) / 2);
        g.setColor(cor);
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, 26, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }
}
