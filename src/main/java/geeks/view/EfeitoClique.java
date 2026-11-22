package geeks.view;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * Animação de clique no estilo Geeks: uma onda se expande a partir do ponto clicado
 * e as "faíscas" radiais (como nos posts da loja) saem do centro e desaparecem.
 */
public class EfeitoClique {
    private static final int DURACAO_MS = 520;

    private final JComponent alvo;
    private Point origem;
    private Color corOnda = Tema.AMARELO;
    private Color corFaisca = Tema.AMARELO;
    private float progresso = 1f;
    private Timer timer;

    public EfeitoClique(JComponent alvo) {
        this.alvo = alvo;
    }

    public boolean emAndamento() {
        return progresso < 1f;
    }

    public void iniciar(Point origem, Color corOnda, Color corFaisca) {
        this.origem = origem;
        this.corOnda = corOnda;
        this.corFaisca = corFaisca;
        this.progresso = 0f;
        if (timer != null) {
            timer.stop();
        }
        final long inicio = System.currentTimeMillis();
        timer = new Timer(15, e -> {
            progresso = Math.min(1f, (System.currentTimeMillis() - inicio) / (float) DURACAO_MS);
            alvo.repaint();
            if (progresso >= 1f) {
                timer.stop();
            }
        });
        timer.start();
    }

    /** Desenha o efeito recortado no formato do componente. Chamar antes de desenhar o texto. */
    public void pintar(Graphics2D g0, Shape recorte) {
        if (origem == null || progresso >= 1f) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.clip(recorte);
        float suave = 1 - (1 - progresso) * (1 - progresso) * (1 - progresso); // desacelera no fim
        float opacidade = 1 - progresso;

        double raio = suave * Math.hypot(alvo.getWidth(), alvo.getHeight());
        g.setColor(comAlfa(corOnda, (int) (120 * opacidade)));
        g.fill(new Ellipse2D.Double(origem.x - raio, origem.y - raio, raio * 2, raio * 2));

        g.setColor(comAlfa(corFaisca, (int) (255 * opacidade)));
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double inicio = 8 + suave * 26;
        double fim = inicio + 6 + suave * 12;
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4 + 0.35;
            g.draw(new Line2D.Double(origem.x + Math.cos(ang) * inicio, origem.y + Math.sin(ang) * inicio,
                    origem.x + Math.cos(ang) * fim, origem.y + Math.sin(ang) * fim));
        }
        g.dispose();
    }

    private static Color comAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alfa)));
    }
}
