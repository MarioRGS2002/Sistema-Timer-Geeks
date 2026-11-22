package geeks.view;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Painel esquerdo do login: fundo amarelo com o logo da Geeks e poucos símbolos dos botões do PlayStation
 * (círculo, X, triângulo e quadrado) subindo devagar, em traço fino e discreto.
 */
public class PainelPlayStation extends JPanel {
    private static final int LARGURA = 380;
    private static final int ALTURA = 560;
    private static final int QUANTIDADE = 14;

    private final List<Simbolo> simbolos = new ArrayList<>();
    private final Random sorteio = new Random(7);
    private final ImageIcon logo = Tema.imagem("/img/geeks_logo.png", 270);
    private Timer timer;

    public PainelPlayStation() {
        setPreferredSize(new Dimension(LARGURA, ALTURA));
        for (int i = 0; i < QUANTIDADE; i++) {
            simbolos.add(new Simbolo(i));
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer = new Timer(25, e -> {
            avancar();
            repaint();
        });
        timer.start();
    }

    @Override
    public void removeNotify() {
        if (timer != null) {
            timer.stop();
        }
        super.removeNotify();
    }

    /** Move todos os símbolos um passo (chamado pelo Timer). */
    public void avancar() {
        for (Simbolo s : simbolos) {
            s.mover();
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g.setColor(Tema.AMARELO);
        g.fillRect(0, 0, w, h);

        // clarão suave atrás do logo
        g.setPaint(new RadialGradientPaint(w / 2f, h / 2f - 20, 230f, new float[]{0f, 1f},
                new Color[]{new Color(255, 255, 255, 70), new Color(255, 255, 255, 0)}));
        g.fillRect(0, 0, w, h);

        for (Simbolo s : simbolos) {
            s.desenhar(g);
        }

        if (logo != null) {
            int x = (w - logo.getIconWidth()) / 2;
            int y = h / 2 - logo.getIconHeight() / 2 - 22;
            logo.paintIcon(this, g, x, y);
            g.setFont(Tema.fonte(Font.BOLD | Font.ITALIC, 18));
            g.setColor(Tema.FUNDO);
            FontMetrics fm = g.getFontMetrics();
            String texto = "Sistema de Gestão";
            g.drawString(texto, (w - fm.stringWidth(texto)) / 2, y + logo.getIconHeight() + 38);
        }
        g.dispose();
    }

    /** Um símbolo flutuante: o tamanho define a "profundidade" (maiores são mais rápidos e mais visíveis). */
    private class Simbolo {
        private final int tipo;
        private final float tamanho;
        private final float velocidade;
        private final float velocidadeGiro;
        private final float fase;
        private final Color cor;
        private float x;
        private float y;
        private float giro;

        Simbolo(int indice) {
            tipo = indice % 4;
            tamanho = 18 + sorteio.nextFloat() * 18;
            float profundidade = (tamanho - 18) / 18f;
            velocidade = 0.3f + 0.5f * profundidade;
            velocidadeGiro = (sorteio.nextFloat() - 0.5f) * 0.012f;
            fase = sorteio.nextFloat() * 6.28f;
            int alfa = (int) (45 + 65 * profundidade);
            cor = indice % 5 == 0 ? new Color(255, 255, 255, alfa + 40) : new Color(13, 13, 13, alfa);
            x = sorteio.nextInt(LARGURA);
            y = sorteio.nextInt(ALTURA);
            giro = sorteio.nextFloat() * 6.28f;
        }

        void mover() {
            int altura = Math.max(getHeight(), ALTURA);
            y -= velocidade;
            x += (float) Math.sin(fase + y / 45f) * 0.35f;
            giro += velocidadeGiro;
            if (y < -tamanho) {
                y = altura + tamanho;
                x = sorteio.nextInt(Math.max(getWidth(), LARGURA));
            }
        }

        void desenhar(Graphics2D g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.translate(x, y);
            g.rotate(giro);
            g.setColor(cor);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double t = tamanho;
            switch (tipo) {
                case 0: // círculo
                    g.draw(new Ellipse2D.Double(-t / 2, -t / 2, t, t));
                    break;
                case 1: // X
                    g.draw(new Line2D.Double(-t * 0.45, -t * 0.45, t * 0.45, t * 0.45));
                    g.draw(new Line2D.Double(-t * 0.45, t * 0.45, t * 0.45, -t * 0.45));
                    break;
                case 2: // triângulo
                    Path2D.Double tri = new Path2D.Double();
                    tri.moveTo(0, -t * 0.55);
                    tri.lineTo(t * 0.5, t * 0.4);
                    tri.lineTo(-t * 0.5, t * 0.4);
                    tri.closePath();
                    g.draw(tri);
                    break;
                default: // quadrado
                    g.draw(new RoundRectangle2D.Double(-t / 2, -t / 2, t, t, 6, 6));
            }
            g.dispose();
        }
    }
}
