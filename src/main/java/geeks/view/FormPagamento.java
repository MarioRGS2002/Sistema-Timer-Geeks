package geeks.view;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.List;

/** Janela de pagamento: o que está sendo pago, o valor e a forma de pagamento. */
public class FormPagamento extends FormularioGeeks {
    private final SeletorOpcoes forma;

    public FormPagamento(String titulo, String descricao, BigDecimal valor, List<String> formas, String textoConfirmar) {
        super(titulo, textoConfirmar, Icone.Tipo.CHECK);
        forma = new SeletorOpcoes(formas, 0);
        JPanel topo = Componentes.transparente(new BorderLayout(0, 6));
        topo.add(Componentes.rotulo("<html>" + descricao + "</html>", Font.PLAIN, 16, Tema.BRANCO), BorderLayout.NORTH);
        topo.add(Componentes.rotulo(Formatador.moeda(valor), Font.BOLD | Font.ITALIC, 38, Tema.AMARELO), BorderLayout.CENTER);
        JPanel tudo = Componentes.transparente(new BorderLayout(0, 18));
        tudo.add(topo, BorderLayout.NORTH);
        tudo.add(bloco("Forma de pagamento", Icone.Tipo.MOEDA, forma), BorderLayout.CENTER);
        tudo.setPreferredSize(new java.awt.Dimension(480, tudo.getPreferredSize().height));
        getCorpo().add(tudo, BorderLayout.CENTER);
        setResumo("O pagamento é registrado no faturamento do dia.");
    }

    public String getForma() {
        return forma.getSelecionada();
    }
}
