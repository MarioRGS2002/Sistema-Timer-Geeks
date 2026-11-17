package geeks.view;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** Ícones vetoriais desenhados com Java2D (não dependem de arquivos de imagem). */
public class Icone implements Icon {
    public enum Tipo {
        MONITOR, CONTROLE, SWITCH, PEAO, CALENDARIO, RELOGIO, COROA, USUARIO_MAIS, RAIO,
        MOEDA, ESTRELA, MAIS, LISTA, LAPIS, CHECK, LUPA, TROFEU, CUBO
    }

    private final Tipo tipo;
    private final int tamanho;
    private final Color cor; // null = usa a cor do componente

    public Icone(Tipo tipo, int tamanho) {
        this(tipo, tamanho, null);
    }

    public Icone(Tipo tipo, int tamanho, Color cor) {
        this.tipo = tipo;
        this.tamanho = tamanho;
        this.cor = cor;
    }

    @Override
    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.scale(tamanho / 24.0, tamanho / 24.0);
        g.setColor(cor != null ? cor : c.getForeground());
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        desenhar(g);
        g.dispose();
    }

    private void desenhar(Graphics2D g) {
        switch (tipo) {
            case MONITOR:
                g.draw(new RoundRectangle2D.Double(3, 4, 18, 12, 3, 3));
                linha(g, 8, 21, 16, 21);
                linha(g, 12, 16, 12, 21);
                break;
            case CONTROLE:
                g.draw(new RoundRectangle2D.Double(2, 7, 20, 11, 9, 9));
                linha(g, 7, 10, 7, 14);
                linha(g, 5, 12, 9, 12);
                g.fill(new Ellipse2D.Double(15, 10, 2, 2));
                g.fill(new Ellipse2D.Double(17.5, 12.5, 2, 2));
                break;
            case SWITCH:
                g.draw(new RoundRectangle2D.Double(2.5, 4, 6, 16, 6, 6));
                g.draw(new RoundRectangle2D.Double(15.5, 4, 6, 16, 6, 6));
                g.draw(new Rectangle2D.Double(9.5, 6, 5, 12));
                g.fill(new Ellipse2D.Double(4.5, 8.5, 2, 2));
                g.fill(new Ellipse2D.Double(17.5, 14.5, 2, 2));
                break;
            case PEAO:
                g.draw(new Ellipse2D.Double(9, 3, 6, 6));
                linha(g, 8, 21, 16, 21);
                linha(g, 10, 21, 11, 13);
                linha(g, 14, 21, 13, 13);
                linha(g, 8, 13, 16, 13);
                break;
            case CALENDARIO:
                g.draw(new RoundRectangle2D.Double(3, 5, 18, 16, 5, 5));
                linha(g, 8, 3, 8, 7);
                linha(g, 16, 3, 16, 7);
                linha(g, 3, 10, 21, 10);
                break;
            case RELOGIO:
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                linha(g, 12, 7, 12, 12);
                linha(g, 12, 12, 15, 14);
                break;
            case COROA:
                poligono(g, 3, 8, 7.5, 12, 12, 5, 16.5, 12, 21, 8, 19, 19, 5, 19);
                break;
            case USUARIO_MAIS:
                g.draw(new Ellipse2D.Double(5, 4, 8, 8));
                g.draw(new Arc2D.Double(2, 14, 14, 14, 0, 180, Arc2D.OPEN));
                linha(g, 19, 8, 19, 14);
                linha(g, 16, 11, 22, 11);
                break;
            case RAIO:
                poligono(g, 13, 2, 4, 14, 11, 14, 10, 22, 19, 10, 12, 10);
                break;
            case MOEDA:
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                linha(g, 12, 6.5, 12, 17.5);
                linha(g, 9.5, 10, 14.5, 10);
                linha(g, 9.5, 14, 14.5, 14);
                break;
            case ESTRELA:
                double[] p = new double[20];
                for (int i = 0; i < 10; i++) {
                    double r = i % 2 == 0 ? 10 : 4.5;
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    p[2 * i] = 12 + r * Math.cos(a);
                    p[2 * i + 1] = 12.5 + r * Math.sin(a);
                }
                poligono(g, p);
                break;
            case MAIS:
                linha(g, 12, 5, 12, 19);
                linha(g, 5, 12, 19, 12);
                break;
            case LISTA:
                linha(g, 8, 6, 21, 6);
                linha(g, 8, 12, 21, 12);
                linha(g, 8, 18, 21, 18);
                g.fill(new Ellipse2D.Double(2.5, 5.2, 1.6, 1.6));
                g.fill(new Ellipse2D.Double(2.5, 11.2, 1.6, 1.6));
                g.fill(new Ellipse2D.Double(2.5, 17.2, 1.6, 1.6));
                break;
            case LAPIS:
                poligono(g, 4, 20, 8, 20, 19, 9, 15, 5, 4, 16);
                break;
            case CHECK:
                Path2D.Double check = new Path2D.Double();
                check.moveTo(5, 12);
                check.lineTo(10, 17);
                check.lineTo(20, 7);
                g.draw(check);
                break;
            case LUPA:
                g.draw(new Ellipse2D.Double(4, 4, 13, 13));
                linha(g, 15.5, 15.5, 20, 20);
                break;
            case TROFEU:
                g.draw(new RoundRectangle2D.Double(8, 3, 8, 9, 5, 5));
                linha(g, 12, 12, 12, 18);
                linha(g, 9, 18, 15, 18);
                linha(g, 8, 21, 16, 21);
                linha(g, 8, 5, 4, 5);
                linha(g, 16, 5, 20, 5);
                break;
            default: // CUBO
                poligono(g, 12, 3, 21, 8, 21, 16, 12, 21, 3, 16, 3, 8);
                linha(g, 3, 8, 12, 13);
                linha(g, 21, 8, 12, 13);
                linha(g, 12, 13, 12, 21);
        }
    }

    private static void linha(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void poligono(Graphics2D g, double... pontos) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(pontos[0], pontos[1]);
        for (int i = 2; i < pontos.length; i += 2) {
            path.lineTo(pontos[i], pontos[i + 1]);
        }
        path.closePath();
        g.draw(path);
    }

    @Override
    public int getIconWidth() {
        return tamanho;
    }

    @Override
    public int getIconHeight() {
        return tamanho;
    }
}
