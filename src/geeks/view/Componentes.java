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

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(tamanho, tamanho);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = suave(g);
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

    /** Barra de progresso fina em degradê amarelo. */
    public static class Barra extends JComponent {
        private final double percentual; // 0..1

        public Barra(double percentual) {
            this.percentual = Math.max(0.04, Math.min(1, percentual));
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
            g2.setPaint(new GradientPaint(0, 0, Tema.AMARELO, getWidth(), 0, new Color(0xFFB800)));
            g2.fillRoundRect(0, 0, (int) (getWidth() * percentual), 8, 8, 8);
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
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
