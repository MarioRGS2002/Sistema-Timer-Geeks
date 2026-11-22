package geeks.view;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/** Aviso, sucesso ou confirmação: ícone colorido que "salta" e mensagem. */
public class FormMensagem extends FormularioGeeks {
    public enum Tom { INFO, SUCESSO, ALERTA }

    private final Componentes.BlocoIcone icone;
    private final Tom tom;

    public FormMensagem(String titulo, String mensagem, Tom tom, Icone.Tipo simbolo, String textoConfirmar, boolean comCancelar) {
        super(titulo, textoConfirmar, Icone.Tipo.CHECK, comCancelar);
        this.tom = tom;
        Color cor = tom == Tom.SUCESSO ? new Color(0x3DDC84) : tom == Tom.ALERTA ? Tema.VERMELHO : Tema.AMARELO;
        icone = new Componentes.BlocoIcone(simbolo, cor, Tema.FUNDO, 64, true).comMargem(12);
        JLabel texto = Componentes.rotulo("<html><body style='width:330px'>" + mensagem.replace("\n", "<br>") + "</body></html>",
                Font.PLAIN, 17, Tema.BRANCO);
        JPanel linha = Componentes.transparente(new BorderLayout(10, 0));
        linha.add(icone, BorderLayout.WEST);
        linha.add(texto, BorderLayout.CENTER);
        getCorpo().add(linha, BorderLayout.CENTER);
    }

    @Override
    public void animarEntrada() {
        super.animarEntrada();
        icone.pop(120);
        if (tom == Tom.ALERTA) {
            icone.agitar(640);
        }
    }
}
