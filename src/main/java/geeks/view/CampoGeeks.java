package geeks.view;

import javax.swing.JTextField;
import java.awt.Graphics;

/** Campo de texto escuro, arredondado, com borda amarela quando em foco e texto de dica. */
public class CampoGeeks extends JTextField {
    private final EstiloCampo estilo;

    public CampoGeeks(String dica) {
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
