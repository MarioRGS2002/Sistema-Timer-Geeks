package geeks.view;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

public class PainelCadastro extends PainelPagina {
    private final CampoGeeks campoNome = new CampoGeeks("Nome completo");
    private final CampoGeeks campoIdade = new CampoGeeks("Idade");
    private final CampoGeeks campoTelefone = new CampoGeeks("(00) 00000-0000");
    private final CampoGeeks campoCpf = new CampoGeeks("000.000.000-00");
    private final CampoGeeks campoResponsavel = new CampoGeeks("Nome do responsável");
    private final JCheckBox chkPlano = new JCheckBox("Escolher um plano depois de salvar");
    private final BotaoGeeks btSalvar = new BotaoGeeks("Salvar cadastro", Icone.Tipo.CHECK, BotaoGeeks.Estilo.PREENCHIDO);
    private final BotaoGeeks btLimpar = new BotaoGeeks("Limpar campos", null, BotaoGeeks.Estilo.CONTORNO);

    public PainelCadastro() {
        super("Cadastro de usuários");
        setLadoDireito(btSalvar);
        JPanel corpo = Componentes.transparente(new GridLayout(1, 2, 26, 0));

        CartaoGeeks form = new CartaoGeeks();
        form.setLayout(new BorderLayout(0, 14));
        form.setBorder(new EmptyBorder(22, 22, 22, 22));
        JPanel campos = Componentes.transparente(new GridLayout(0, 1, 0, 12));
        JPanel linha = Componentes.transparente(new GridLayout(1, 2, 16, 0));
        linha.add(comRotulo("Idade", campoIdade));
        linha.add(comRotulo("Telefone", campoTelefone));
        campos.add(comRotulo("Nome", campoNome));
        campos.add(linha);
        campos.add(comRotulo("CPF", campoCpf));
        campos.add(comRotulo("Responsável (opcional, obrigatório para menores de 18)", campoResponsavel));
        JPanel envoltorio = Componentes.transparente(new BorderLayout());
        envoltorio.add(campos, BorderLayout.NORTH);
        JPanel rodape = Componentes.transparente(new BorderLayout());
        rodape.add(btLimpar, BorderLayout.WEST);
        form.add(Componentes.rotuloIcone("Dados do cliente", Icone.Tipo.USUARIO_MAIS, Tema.BRANCO, 22), BorderLayout.NORTH);
        form.add(envoltorio, BorderLayout.CENTER);
        form.add(rodape, BorderLayout.SOUTH);

        corpo.add(form);
        corpo.add(criarCartaoClube());
        getCorpo().add(corpo, BorderLayout.CENTER);
    }

    private JPanel comRotulo(String rotulo, CampoGeeks campo) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 6));
        p.add(Componentes.rotulo(rotulo, Font.BOLD, 14, Tema.BRANCO), BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    /** Em vez de repetir "Cliente já cadastrado?", o cadastro convida a entrar no clube logo após salvar. */
    private CartaoGeeks criarCartaoClube() {
        CartaoGeeks c = new CartaoGeeks(Tema.AMARELO, Tema.AMARELO, false);
        c.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(8, 28, 8, 28);

        g.gridy = 0;
        g.insets = new Insets(0, 0, 18, 0);
        c.add(new Componentes.Faixa(), g);
        g.insets = new Insets(8, 28, 8, 28);
        g.gridy = 1;
        g.fill = GridBagConstraints.NONE;
        c.add(new Componentes.BlocoIcone(Icone.Tipo.COROA, Tema.FUNDO, Tema.AMARELO, 84, true), g);
        g.gridy = 2;
        c.add(Componentes.rotulo("CLUBE GEEKS", Font.BOLD | Font.ITALIC, 38, Tema.FUNDO), g);
        g.gridy = 3;
        c.add(Componentes.rotulo("Vantagens para clientes Geeks", Font.BOLD, 16, Tema.FUNDO), g);
        g.gridy = 4;
        chkPlano.setOpaque(false);
        chkPlano.setFont(Tema.fonte(Font.BOLD, 15));
        chkPlano.setForeground(Tema.FUNDO);
        c.add(chkPlano, g);
        return c;
    }

    public String getNome() {
        return campoNome.getText();
    }

    public String getIdade() {
        return campoIdade.getText();
    }

    public String getTelefone() {
        return campoTelefone.getText();
    }

    public String getCpf() {
        return campoCpf.getText();
    }

    public String getResponsavel() {
        return campoResponsavel.getText();
    }

    public boolean isEscolherPlano() {
        return chkPlano.isSelected();
    }

    public void limparCampos() {
        campoNome.setText("");
        campoIdade.setText("");
        campoTelefone.setText("");
        campoCpf.setText("");
        campoResponsavel.setText("");
        chkPlano.setSelected(false);
    }

    public void addListenerSalvar(ActionListener l) {
        btSalvar.addActionListener(l);
    }

    public void addListenerLimpar(ActionListener l) {
        btLimpar.addActionListener(l);
    }
}
