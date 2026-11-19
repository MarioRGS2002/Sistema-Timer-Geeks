package geeks.view;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** Pequenos componentes visuais reutilizados em todas as telas. */
public final class Componentes {
    private Componentes() {
    }

    public static JPanel transparente(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static JLabel rotulo(String texto, int estilo, float tamanho, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fonte(estilo, tamanho));
        l.setForeground(cor);
        return l;
    }

    public static JLabel rotuloIcone(String texto, Icone.Tipo tipo, Color cor, float tamanho) {
        JLabel l = rotulo(texto, Font.BOLD, tamanho, cor);
        l.setIcon(new Icone(tipo, (int) tamanho + 2, cor));
        l.setIconTextGap(6);
        return l;
    }

    private static Graphics2D suave(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** Bloco colorido com um ícone dentro (identifica o dispositivo, o plano etc.). */
    public static class BlocoIcone extends JComponent {
        private final Icone.Tipo tipo;
        private final Color fundo;
        private final Color corIcone;
        private final int tamanho;
        private final boolean circular;
        private int margem;
        private double escala = 1;
        private double rotacao = 0;

        public BlocoIcone(Icone.Tipo tipo, Color fundo, Color corIcone, int tamanho, boolean circular) {
            this.tipo = tipo;
            this.fundo = fundo;
            this.corIcone = corIcone;
            this.tamanho = tamanho;
            this.circular = circular;
        }

        public BlocoIcone(geeks.model.TipoDispositivo dispositivo, int tamanho) {
            this(Tema.iconeDe(dispositivo), Tema.corDe(dispositivo), Tema.FUNDO, tamanho, false);
        }

        /** Espaço extra em volta, para as animações (pop, agitar) não serem cortadas. */
        public BlocoIcone comMargem(int margem) {
            this.margem = margem;
            return this;
        }

        /** O bloco "salta" para dentro da tela com um leve exagero. */
        public void pop(int atrasoMs) {
            escala = 0;
            Animacao.executar(520, atrasoMs, p -> {
                escala = Animacao.elastico(p);
                repaint();
            }, () -> {
                escala = 1;
                repaint();
            });
        }

        /** Balança de um lado para o outro e assenta (usado em avisos). */
        public void agitar(int atrasoMs) {
            Animacao.executar(560, atrasoMs, p -> {
                rotacao = Math.sin(p * Math.PI * 5) * (1 - p) * 0.28;
                repaint();
            }, () -> {
                rotacao = 0;
                repaint();
            });
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(tamanho + 2 * margem, tamanho + 2 * margem);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
            g2.translate(getWidth() / 2.0, getHeight() / 2.0);
            g2.scale(escala, escala);
            g2.rotate(rotacao);
            g2.translate(-tamanho / 2.0, -tamanho / 2.0);
            g2.setColor(fundo);
            int arco = circular ? tamanho : (int) (tamanho * 0.32);
            g2.fillRoundRect(0, 0, tamanho, tamanho, arco, arco);
            int t = (int) (tamanho * 0.55);
            new Icone(tipo, t, corIcone).paintIcon(this, g2, (tamanho - t) / 2, (tamanho - t) / 2);
            g2.dispose();
        }
    }

    /** Etiqueta arredondada (ex.: nome do dispositivo). */
    public static class Chip extends JComponent {
        private final String texto;
        private final Color cor;
        private final Icone.Tipo icone;

        public Chip(String texto, Color cor, Icone.Tipo icone) {
            this.texto = texto;
            this.cor = cor;
            this.icone = icone;
            setFont(Tema.fonte(Font.BOLD, 13));
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(texto) + 22 + (icone != null ? 20 : 0), 24);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
            g2.setColor(cor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            int x = 11;
            if (icone != null) {
                new Icone(icone, 14, Tema.FUNDO).paintIcon(this, g2, x, (getHeight() - 14) / 2);
                x += 20;
            }
            g2.setColor(Tema.FUNDO);
            g2.setFont(getFont());
            g2.drawString(texto, x, (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Barra de progresso fina em degradê amarelo (pode "crescer" ao aparecer). */
    public static class Barra extends JComponent {
        private final double alvo; // 0..1
        private final int atrasoMs;
        private double atual;
        private boolean iniciou;

        public Barra(double percentual) {
            this(percentual, -1);
        }

        /** atrasoMs >= 0 faz a barra crescer do zero até o valor ao aparecer na tela. */
        public Barra(double percentual, int atrasoMs) {
            this.alvo = Math.max(0.04, Math.min(1, percentual));
            this.atrasoMs = atrasoMs;
            this.atual = atrasoMs < 0 ? alvo : 0;
        }

        @Override
        public void addNotify() {
            super.addNotify();
            if (atrasoMs >= 0 && !iniciou) {
                iniciou = true;
                Animacao.executar(700, atrasoMs, p -> {
                    atual = alvo * Animacao.suave(p);
                    repaint();
                }, null);
            }
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, 8);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
            g2.setColor(Tema.BORDA);
            g2.fillRoundRect(0, 0, getWidth(), 8, 8, 8);
            int largura = (int) (getWidth() * atual);
            if (largura > 0) {
                g2.setPaint(new GradientPaint(0, 0, Tema.AMARELO, getWidth(), 0, new Color(0xFFB800)));
                g2.fillRoundRect(0, 0, Math.max(8, largura), 8, 8, 8);
            }
            g2.dispose();
        }
    }

    /** Faixa colorida usada nos posts da Geeks (Dia das Crianças). */
    public static class Faixa extends JComponent {
        private static final Color[] CORES = {new Color(0xFF3B3B), new Color(0xFFB800), new Color(0x3DDC84),
                new Color(0x2E8BFF), new Color(0xA259FF)};

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, 10);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
            int w = getWidth();
            for (int i = 0; i < CORES.length; i++) {
                g2.setColor(CORES[i]);
                g2.fillRect(i * w / CORES.length, 0, w / CORES.length + 1, getHeight());
            }
            g2.dispose();
        }
    }

    /** Indicador pequeno com ícone e texto (ex.: "5 ativas"). */
    public static class Pilula extends JLabel {
        private float brilho;

        /** Contorno amarelo que acende e apaga: chama a atenção para uma mudança de valor. */
        public void piscar() {
            Animacao.executar(800, 0, p -> {
                brilho = (float) (1 - p);
                repaint();
            }, () -> {
                brilho = 0;
                repaint();
            });
        }

        public Pilula(Icone.Tipo tipo, String texto) {
            super(texto, new Icone(tipo, 18, Tema.AMARELO), SwingConstants.LEFT);
            setFont(Tema.fonte(Font.BOLD, 15));
            setForeground(Tema.BRANCO);
            setIconTextGap(8);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 16, 8, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
            g2.setColor(new Color(0x222222));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setColor(new Color(0x333333));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            if (brilho > 0) {
                g2.setColor(new Color(255, 214, 10, (int) (255 * brilho)));
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
