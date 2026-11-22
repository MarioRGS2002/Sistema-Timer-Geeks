package geeks.view;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/** Painel de cantos arredondados usado como "cartão", com animações de entrada, saída e hover. */
public class CartaoGeeks extends JPanel {
    private final Color fundo;
    private final Color borda;
    private final boolean tracejado;
    private float alfa = 1f;
    private float escala = 1f;
    private float hover = 0f;
    private boolean hoverAlvo;
    private Timer timerHover;

    public CartaoGeeks() {
        this(Tema.CARTAO, Tema.BORDA, false);
    }

    public CartaoGeeks(Color fundo, Color borda, boolean tracejado) {
        this.fundo = fundo;
        this.borda = borda;
        this.tracejado = tracejado;
        setOpaque(false);
    }

    /** O cartão aparece com fade e um pequeno "pop". */
    public void entrar(int atrasoMs) {
        alfa = 0f;
        escala = 0.94f;
        Animacao.executar(360, atrasoMs, p -> {
            alfa = (float) Animacao.suave(p);
            escala = (float) (0.94 + 0.06 * Animacao.elastico(p));
            repaint();
        }, () -> {
            alfa = 1f;
            escala = 1f;
            repaint();
        });
    }

    /** O cartão some encolhendo levemente; depois executa a ação informada. */
    public void sair(Runnable depois) {
        Animacao.executar(240, 0, p -> {
            alfa = (float) (1 - p);
            escala = (float) (1 - 0.08 * p);
            repaint();
        }, depois);
    }

    /** Ativa o realce da borda quando o mouse passa sobre o cartão (chamar depois de montar os filhos). */
    public void ativarHover() {
        MouseListener ml = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                atualizar(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                atualizar(e);
            }

            private void atualizar(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), CartaoGeeks.this);
                mudarHover(contains(p));
            }
        };
        instalar(this, ml);
    }

    private void instalar(Component c, MouseListener ml) {
        c.addMouseListener(ml);
        if (c instanceof Container) {
            for (Component f : ((Container) c).getComponents()) {
                instalar(f, ml);
            }
        }
    }

    private void mudarHover(boolean alvo) {
        if (alvo == hoverAlvo) {
            return;
        }
        hoverAlvo = alvo;
        if (timerHover != null) {
            timerHover.stop();
        }
        final float de = hover;
        timerHover = Animacao.executar(160, 0, p -> {
            hover = (float) (de + ((alvo ? 1 : 0) - de) * Animacao.suave(p));
            repaint();
        }, null);
    }

    @Override
    public void paint(Graphics g0) {
        if (alfa >= 0.999f && escala >= 0.999f) {
            super.paint(g0);
            return;
        }
        if (alfa <= 0.01f || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gi = img.createGraphics();
        super.paint(gi);
        gi.dispose();
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, alfa)));
        g.translate(getWidth() / 2.0, getHeight() / 2.0);
        g.scale(escala, escala);
        g.translate(-getWidth() / 2.0, -getHeight() / 2.0);
        g.drawImage(img, 0, 0, null);
        g.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!tracejado) {
            g2.setColor(Tema.misturar(fundo, Tema.BRANCO, hover * 0.04));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
        }
        g2.setColor(Tema.misturar(borda, Tema.AMARELO, hover * 0.75));
        g2.setStroke(tracejado
                ? new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 6f}, 0f)
                : new BasicStroke(1f + hover));
        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 28, 28);
        g2.dispose();
    }
}
