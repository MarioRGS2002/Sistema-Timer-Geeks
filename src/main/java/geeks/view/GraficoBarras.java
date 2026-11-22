package geeks.view;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Gráfico de barras limpo: poucas linhas de grade, barras amarelas que "crescem" ao aparecer e um balão
 * com o valor exato ao passar o mouse.
 */
public class GraficoBarras extends JComponent {
    private static final int ESQ = 64;
    private static final int DIR = 12;
    private static final int TOPO = 26;
    private static final int BASE = 34;

    private List<String> rotulos = new ArrayList<>();
    private List<String> completos = new ArrayList<>();
    private List<BigDecimal> valores = new ArrayList<>();
    private double progresso = 1;
    private int hover = -1;
    private Timer timer;

    public GraficoBarras() {
        setOpaque(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int novo = indiceEm(e.getX());
                if (novo != hover) {
                    hover = novo;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void definir(List<String> rotulos, List<String> rotulosCompletos, List<BigDecimal> valores) {
        this.rotulos = rotulos;
        this.completos = rotulosCompletos;
        this.valores = valores;
        this.hover = -1;
        reanimar();
    }

    /** As barras crescem de baixo para cima, uma depois da outra. */
    public void reanimar() {
        if (timer != null) {
            timer.stop();
        }
        progresso = 0;
        timer = Animacao.executar(850, 0, p -> {
            progresso = p;
            repaint();
        }, () -> {
            progresso = 1;
            repaint();
        });
    }

    private int indiceEm(int x) {
        int n = rotulos.size();
        int larguraPlot = getWidth() - ESQ - DIR;
        if (n == 0 || x < ESQ || x > ESQ + larguraPlot) {
            return -1;
        }
        return Math.min(n - 1, (int) ((x - ESQ) * n / (double) larguraPlot));
    }

    private static double passoBonito(double bruto) {
        double exp = Math.floor(Math.log10(bruto));
        double base = Math.pow(10, exp);
        double f = bruto / base;
        double bonito = f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10;
        return bonito * base;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int plotX = ESQ, plotY = TOPO, plotW = w - ESQ - DIR, plotH = h - TOPO - BASE;
        int n = rotulos.size();
        double max = 0;
        boolean temDados = false;
        for (BigDecimal v : valores) {
            if (v != null) {
                temDados = true;
                max = Math.max(max, v.doubleValue());
            }
        }
        if (plotW <= 0 || plotH <= 0 || n == 0 || !temDados) {
            g.setFont(Tema.fonte(Font.PLAIN, 17));
            g.setColor(Tema.CINZA);
            String msg = "Ainda não há faturamento neste período.";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            g.dispose();
            return;
        }

        double passo = passoBonito(max / 4.0);
        int marcas = Math.max(2, (int) Math.ceil(max / passo));
        double topoValor = passo * marcas;

        // linhas de grade e valores do eixo Y
        g.setFont(Tema.fonte(Font.PLAIN, 12));
        FontMetrics fmY = g.getFontMetrics();
        for (int i = 0; i <= marcas; i++) {
            int y = (int) Math.round(plotY + plotH - plotH * (double) i / marcas);
            g.setColor(i == 0 ? new Color(0x4A4A4A) : new Color(0x262626));
            g.drawLine(plotX, y, plotX + plotW, y);
            String t = "R$ " + Math.round(i * passo);
            g.setColor(Tema.CINZA);
            g.drawString(t, plotX - 12 - fmY.stringWidth(t), y + 4);
        }

        double slot = plotW / (double) n;
        if (hover >= 0 && hover < n) {
            g.setColor(new Color(255, 255, 255, 12));
            g.fillRoundRect((int) (plotX + slot * hover) + 1, plotY, (int) slot - 2, plotH, 10, 10);
        }

        // barras (cortadas na linha de base para ficarem com o topo arredondado e a base reta)
        Shape clipOriginal = g.getClip();
        g.clipRect(0, 0, w, plotY + plotH);
        double larg = Math.min(46, slot * 0.58);
        double d = 0.45;
        for (int i = 0; i < n; i++) {
            BigDecimal v = valores.get(i);
            if (v == null) {
                continue;
            }
            double local = Math.max(0, Math.min(1, (progresso - d * i / n) / (1 - d)));
            double alt = plotH * (v.doubleValue() / topoValor) * Animacao.suave(local);
            if (alt < 1) {
                continue;
            }
            double x = plotX + slot * i + (slot - larg) / 2;
            double y = plotY + plotH - alt;
            double raio = Math.min(larg / 2, 9);
            Color cima = i == hover ? Tema.misturar(Tema.AMARELO, Tema.BRANCO, 0.35) : Tema.AMARELO;
            Color baixo = Tema.misturar(cima, new Color(0xB88A00), 0.35);
            g.setPaint(new GradientPaint(0, (float) y, cima, 0, (float) (plotY + plotH), baixo));
            g.fill(new RoundRectangle2D.Double(x, y, larg, alt + raio + 2, raio * 2, raio * 2));
        }
        g.setClip(clipOriginal);

        // rótulos do eixo X (pula alguns se estiverem apertados)
        g.setFont(Tema.fonte(Font.PLAIN, 12));
        FontMetrics fmX = g.getFontMetrics();
        int maiorRotulo = 0;
        for (String r : rotulos) {
            maiorRotulo = Math.max(maiorRotulo, fmX.stringWidth(r));
        }
        int pula = Math.max(1, (int) Math.ceil((maiorRotulo + 10) / slot));
        for (int i = 0; i < n; i++) {
            if (i % pula != 0 && i != hover) {
                continue;
            }
            g.setFont(Tema.fonte(i == hover ? Font.BOLD : Font.PLAIN, 12));
            g.setColor(i == hover ? Tema.BRANCO : Tema.CINZA);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(rotulos.get(i), (float) (plotX + slot * (i + 0.5) - fm.stringWidth(rotulos.get(i)) / 2.0), plotY + plotH + 22);
        }

        // balão com o valor exato
        if (hover >= 0 && hover < n && valores.get(hover) != null && progresso > 0.9) {
            desenharBalao(g, hover, valores.get(hover), topoValor, plotX, plotY, plotW, plotH, slot, w);
        }
        g.dispose();
    }

    private void desenharBalao(Graphics2D g, int i, BigDecimal valor, double topoValor, int plotX, int plotY, int plotW,
                               int plotH, double slot, int w) {
        String linha1 = completos.get(i);
        String linha2 = Formatador.moeda(valor);
        g.setFont(Tema.fonte(Font.PLAIN, 13));
        int w1 = g.getFontMetrics().stringWidth(linha1);
        g.setFont(Tema.fonte(Font.BOLD | Font.ITALIC, 20));
        int w2 = g.getFontMetrics().stringWidth(linha2);
        int bw = Math.max(w1, w2) + 30, bh = 58;
        double alturaBarra = plotH * (valor.doubleValue() / topoValor);
        double topoBarra = plotY + plotH - alturaBarra;
        int bx = (int) (plotX + slot * (i + 0.5) - bw / 2.0);
        bx = Math.max(plotX, Math.min(bx, w - DIR - bw));
        int by = (int) (topoBarra - bh - 12);
        if (by < 2) {
            by = (int) topoBarra + 12;
        }
        g.setColor(new Color(0x242424));
        g.fillRoundRect(bx, by, bw, bh, 16, 16);
        g.setColor(new Color(255, 214, 10, 170));
        g.setStroke(new java.awt.BasicStroke(1.5f));
        g.drawRoundRect(bx, by, bw, bh, 16, 16);
        g.setFont(Tema.fonte(Font.PLAIN, 13));
        g.setColor(Tema.CINZA);
        g.drawString(linha1, bx + (bw - w1) / 2, by + 22);
        g.setFont(Tema.fonte(Font.BOLD | Font.ITALIC, 20));
        g.setColor(Tema.AMARELO);
        g.drawString(linha2, bx + (bw - w2) / 2, by + 46);
    }
}
