package geeks.view;

import geeks.model.PeriodoFaturamento;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Controle segmentado "Dia | Semana | Mês": a pílula amarela desliza até a opção escolhida. */
public class SegmentoPeriodo extends JComponent {
    private static final PeriodoFaturamento[] ITENS = PeriodoFaturamento.values();
    private static final int LARGURA_ITEM = 96;

    private final List<ActionListener> ouvintes = new ArrayList<>();
    private int selecionado = 1;
    private double posicao = 1;
    private int passando = -1;
    private Timer timer;

    public SegmentoPeriodo() {
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                selecionar(indiceEm(e.getX()), true);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                passando = indiceEm(e.getX());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                passando = -1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    selecionar(selecionado - 1, true);
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    selecionar(selecionado + 1, true);
                }
            }
        });
    }

    private int indiceEm(int x) {
        int i = (int) ((x - 4) / ((getWidth() - 8) / (double) ITENS.length));
        return Math.max(0, Math.min(ITENS.length - 1, i));
    }

    private void selecionar(int indice, boolean notificar) {
        if (indice < 0 || indice >= ITENS.length || indice == selecionado) {
            return;
        }
        selecionado = indice;
        if (timer != null) {
            timer.stop();
        }
        final double de = posicao;
        timer = Animacao.executar(300, 0, p -> {
            posicao = de + (indice - de) * Animacao.elastico(p);
            repaint();
        }, () -> {
            posicao = indice;
            repaint();
        });
        if (notificar) {
            for (ActionListener l : ouvintes) {
                l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ITENS[indice].name()));
            }
        }
    }

    public PeriodoFaturamento getPeriodo() {
        return ITENS[selecionado];
    }

    /** O actionCommand do evento é o nome do PeriodoFaturamento (DIA, SEMANA ou MES). */
    public void addListenerPeriodo(ActionListener l) {
        ouvintes.add(l);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ITENS.length * LARGURA_ITEM + 8, 44);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight(), pad = 4;
        double larguraItem = (w - 2 * pad) / (double) ITENS.length;

        g.setColor(new Color(0x1C1C1C));
        g.fillRoundRect(0, 0, w, h, h, h);
        g.setColor(isFocusOwner() ? Tema.AMARELO : new Color(0x333333));
        g.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.5f));
        g.drawRoundRect(1, 1, w - 3, h - 3, h, h);

        g.setColor(Tema.AMARELO);
        g.fillRoundRect((int) (pad + posicao * larguraItem), pad, (int) larguraItem, h - 2 * pad, h - 2 * pad, h - 2 * pad);

        g.setFont(Tema.fonte(Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < ITENS.length; i++) {
            Color cor = Math.abs(posicao - i) < 0.5 ? Tema.FUNDO : passando == i ? Tema.BRANCO : Tema.CINZA;
            g.setColor(cor);
            String texto = ITENS[i].getRotulo();
            double cx = pad + larguraItem * (i + 0.5);
            g.drawString(texto, (float) (cx - fm.stringWidth(texto) / 2.0), (h + fm.getAscent() - fm.getDescent()) / 2f);
        }
        g.dispose();
    }
}
