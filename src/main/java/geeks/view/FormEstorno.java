package geeks.view;

import geeks.dados.CaixaDAO;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;

/** Estornar um pagamento: quanto devolver (total ou parte) e o motivo (fica registrado com o nome do gerente). */
public class FormEstorno extends FormularioGeeks {
    private final CampoGeeks valor = new CampoGeeks("0,00");
    private final CampoGeeks motivo = new CampoGeeks("Ex.: cobrado em dobro, forma de pagamento errada...");

    public FormEstorno(CaixaDAO.Pagamento p) {
        super("Estornar pagamento", "Estornar", Icone.Tipo.SETA_ESQUERDA);
        BigDecimal restante = p.valor.subtract(p.estornado);
        valor.setText(restante.toPlainString().replace('.', ','));
        JPanel campos = Componentes.transparente(new GridLayout(0, 1, 0, 12));
        campos.add(Componentes.rotulo("<html><b>" + (p.cliente == null ? "-" : p.cliente) + "</b>  ·  "
                + Formatador.moeda(p.valor) + " em " + (p.forma == null ? "-" : p.forma)
                + (p.estornado.signum() > 0 ? "  ·  já estornado " + Formatador.moeda(p.estornado) : "") + "</html>",
                Font.PLAIN, 16, Tema.BRANCO));
        campos.add(comRotulo("Valor a estornar (até " + Formatador.moeda(restante) + ")", valor));
        campos.add(comRotulo("Motivo (obrigatório)", motivo));
        JPanel envolve = Componentes.transparente(new BorderLayout());
        envolve.add(campos, BorderLayout.NORTH);
        envolve.setPreferredSize(new java.awt.Dimension(520, envolve.getPreferredSize().height));
        getCorpo().add(envolve, BorderLayout.CENTER);
        setResumo("O pagamento original continua no histórico; o estorno é descontado do caixa de hoje.");
    }

    private JPanel comRotulo(String rotulo, CampoGeeks campo) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 6));
        p.add(Componentes.rotulo(rotulo, Font.BOLD, 14, Tema.BRANCO), BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    public String getValor() {
        return valor.getText();
    }

    public String getMotivo() {
        return motivo.getText();
    }
}
