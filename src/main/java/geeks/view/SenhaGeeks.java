package geeks.view;

import javax.swing.JPasswordField;
import java.awt.Graphics;

/** Campo de senha com a mesma aparência do CampoGeeks. */
public class SenhaGeeks extends JPasswordField {
    private final EstiloCampo estilo;

    public SenhaGeeks(String dica) {
        this.estilo = new EstiloCampo(this, dica);
    }

    public void setIconeEsquerda(Icone.Tipo icone) {
        estilo.setIcone(icone);
    }

    @Override
    protected void paintComponent(Graphics g) {
        estilo.pintarFundo(g);
        super.paintComponent(g);
        estilo.pintarDica(g);
    }
}
