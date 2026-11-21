package geeks.view;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cartão de resumo: ícone, título pequeno e valor grande (o valor "conta" até o novo número). */
public class CartaoResumo extends CartaoGeeks {
    private final JLabel lbTitulo;
    private String tituloBase;
    private String complemento = "";
    private final JLabel lbValor = Componentes.rotulo(" ", Font.BOLD | Font.ITALIC, 28, Tema.BRANCO);
    private BigDecimal exibido = BigDecimal.ZERO;
    private Timer timer;

    public CartaoResumo(String titulo, Icone.Tipo icone) {
        setLayout(new BorderLayout(14, 0));
        setBorder(new EmptyBorder(14, 18, 14, 18));
        this.tituloBase = titulo;
        lbTitulo = Componentes.rotulo(titulo, Font.PLAIN, 14, Tema.CINZA);
        JPanel textos = Componentes.transparente(new GridLayout(2, 1, 0, 0));
        textos.add(lbTitulo);
        textos.add(lbValor);
        add(new Componentes.BlocoIcone(icone, Tema.AMARELO, Tema.FUNDO, 48, false), BorderLayout.WEST);
        add(textos, BorderLayout.CENTER);
        ativarHover();
    }

    public void setTitulo(String titulo) {
        this.tituloBase = titulo;
        this.complemento = "";
        atualizarTitulo();
    }

    private void atualizarTitulo() {
        lbTitulo.setText(complemento.isEmpty() ? tituloBase
                : "<html>" + tituloBase + " <span style='color:#FFD60A'>&nbsp;&middot;&nbsp; " + complemento + "</span></html>");
    }

    /** Volta o contador para zero (usado quando a tela é aberta, para a contagem recomeçar). */
    public void zerar() {
        exibido = BigDecimal.ZERO;
    }

    public void animarValor(BigDecimal alvo) {
        if (timer != null) {
            timer.stop();
        }
        final BigDecimal de = exibido;
        exibido = alvo;
        timer = Animacao.executar(650, 0, p -> {
            BigDecimal v = de.add(alvo.subtract(de).multiply(BigDecimal.valueOf(Animacao.suave(p)))).setScale(2, RoundingMode.HALF_UP);
            lbValor.setText(Formatador.moeda(v));
        }, () -> lbValor.setText(Formatador.moeda(alvo)));
    }

    /** Texto em destaque no lugar do número; o complemento (ex.: um valor) vai em amarelo ao lado do título. */
    public void definirTexto(String principal, String complemento) {
        if (timer != null) {
            timer.stop();
        }
        lbValor.setText(principal);
        this.complemento = complemento;
        atualizarTitulo();
    }
}
