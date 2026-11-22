package geeks.view;

import geeks.model.TipoDispositivo;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Identidade visual da Geeks: cores, fontes e imagens. */
public final class Tema {
    public static final Color FUNDO = new Color(0x0D0D0D);
    public static final Color LATERAL = new Color(0x161616);
    public static final Color CARTAO = new Color(0x181818);
    public static final Color BORDA = new Color(0x2E2E2E);
    public static final Color CAMPO = new Color(0x0F0F0F);
    public static final Color AMARELO = new Color(0xFFD60A);
    public static final Color BRANCO = Color.WHITE;
    public static final Color CINZA = new Color(0xB8B8B8);
    public static final Color AZUL = new Color(0x5B9BFF);
    public static final Color VERMELHO = new Color(0xFF5A5F);

    private static final String FAMILIA = escolherFamilia();

    private Tema() {
    }

    private static String escolherFamilia() {
        List<String> instaladas = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : new String[]{"Barlow", "Segoe UI", "Helvetica Neue"}) {
            if (instaladas.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    /** Mistura duas cores (t = 0 devolve a, t = 1 devolve b). */
    public static Color misturar(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    public static Font fonte(int estilo, float tamanho) {
        return new Font(FAMILIA, estilo, 1).deriveFont(tamanho);
    }

    /** Carrega uma imagem do classpath (ex.: "/img/geeks_logo.png"); devolve null se não existir. */
    public static ImageIcon imagem(String caminho, int largura) {
        URL url = Tema.class.getResource(caminho);
        if (url == null) {
            return null;
        }
        ImageIcon original = new ImageIcon(url);
        int altura = original.getIconHeight() * largura / original.getIconWidth();
        return new ImageIcon(original.getImage().getScaledInstance(largura, altura, Image.SCALE_AREA_AVERAGING));
    }

    /** Ícone da janela e da barra de tarefas: o "g" da Geeks (letras brancas) em vários tamanhos. */
    public static List<Image> iconesDaJanela() {
        List<Image> icones = new ArrayList<>();
        Image base = imagemOriginal("/img/g_geeks_icone.png");
        if (base != null) {
            for (int lado : new int[]{16, 24, 32, 48, 64, 128, 256}) {
                icones.add(new ImageIcon(base.getScaledInstance(lado, lado, Image.SCALE_AREA_AVERAGING)).getImage());
            }
        }
        return icones;
    }

    public static Image imagemOriginal(String caminho) {
        URL url = Tema.class.getResource(caminho);
        return url == null ? null : new ImageIcon(url).getImage();
    }

    public static Color corDe(TipoDispositivo tipo) {
        switch (tipo) {
            case PLAYSTATION:
                return AZUL;
            case SWITCH:
                return VERMELHO;
            case XADREZ:
                return BRANCO;
            default:
                return AMARELO;
        }
    }

    public static Icone.Tipo iconeDe(TipoDispositivo tipo) {
        switch (tipo) {
            case PLAYSTATION:
                return Icone.Tipo.CONTROLE;
            case SWITCH:
                return Icone.Tipo.SWITCH;
            case XADREZ:
                return Icone.Tipo.PEAO;
            default:
                return Icone.Tipo.MONITOR;
        }
    }
}
