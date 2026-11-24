package geeks.view;

import geeks.model.Cliente;
import geeks.model.FichaCliente;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

/**
 * Corrigir o cadastro do cliente (aberto pela ficha "Dados do usuário"). O CPF não muda; os demais
 * campos usam as mesmas máscaras e regras do cadastro.
 */
public class FormEditarCliente extends FormularioGeeks {
    private final CampoGeeks nome = new CampoGeeks("Nome completo");
    private final CampoGeeks nascimento = new CampoGeeks("dd/mm/aaaa");
    private final CampoGeeks telefone = new CampoGeeks("(00) 00000-0000");
    private final CampoGeeks respNome = new CampoGeeks("Nome do responsável");
    private final CampoGeeks parentesco = new CampoGeeks("Mãe, pai, tio...");
    private final CampoGeeks respCpf = new CampoGeeks("000.000.000-00");
    private final CampoGeeks respTelefone = new CampoGeeks("(00) 00000-0000");

    /** @param valores o que foi digitado na tentativa anterior (null = dados atuais do cliente) */
    public FormEditarCliente(FichaCliente f, String[] valores) {
        super("Editar cliente", "Salvar alterações", Icone.Tipo.CHECK);
        Mascara.DATA.aplicar(nascimento);
        Mascara.TELEFONE.aplicar(telefone);
        Mascara.CPF.aplicar(respCpf);
        Mascara.TELEFONE.aplicar(respTelefone);
        Cliente c = f.getCliente();
        if (valores == null) {
            nome.setText(c.getNome());
            nascimento.setText(c.getDataNascimento() == null ? "" : c.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            telefone.setText(c.getTelefone());
            respNome.setText(f.getRespNome() == null ? "" : f.getRespNome());
            parentesco.setText(f.getRespParentesco() == null ? "" : f.getRespParentesco());
            respCpf.setText(f.getRespCpf() == null ? "" : f.getRespCpf());
            respTelefone.setText(f.getRespTelefone() == null ? "" : f.getRespTelefone());
        } else {
            CampoGeeks[] campos = {nome, nascimento, telefone, respNome, parentesco, respCpf, respTelefone};
            for (int i = 0; i < campos.length && i < valores.length; i++) {
                campos[i].setText(valores[i]);
            }
        }

        JPanel campos = Componentes.transparente(new GridLayout(0, 1, 0, 12));
        campos.add(Componentes.rotulo("CPF " + Formatador.cpf(c.getCpf()) + " (não muda)", Font.BOLD, 15, Tema.CINZA));
        campos.add(comRotulo("Nome", nome));
        campos.add(dupla(comRotulo("Data de nascimento", nascimento), comRotulo("Telefone", telefone)));
        campos.add(dupla(comRotulo("Responsável (se menor de 18)", respNome), comRotulo("Parentesco", parentesco)));
        campos.add(dupla(comRotulo("CPF do responsável", respCpf), comRotulo("Telefone do responsável", respTelefone)));
        JPanel envolve = Componentes.transparente(new BorderLayout());
        envolve.add(campos, BorderLayout.NORTH);
        envolve.setPreferredSize(new java.awt.Dimension(600, envolve.getPreferredSize().height));
        getCorpo().add(envolve, BorderLayout.CENTER);
        setResumo("As alterações valem a partir de agora; o histórico continua ligado ao mesmo cliente.");
    }

    private JPanel dupla(JPanel a, JPanel b) {
        JPanel p = Componentes.transparente(new GridLayout(1, 2, 16, 0));
        p.add(a);
        p.add(b);
        return p;
    }

    private JPanel comRotulo(String rotulo, CampoGeeks campo) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 6));
        p.add(Componentes.rotulo(rotulo, Font.BOLD, 14, Tema.BRANCO), BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    /** {nome, nascimento, telefone, responsável, parentesco, CPF do responsável, telefone do responsável} */
    public String[] getValores() {
        return new String[]{nome.getText(), nascimento.getText(), telefone.getText(), respNome.getText(),
                parentesco.getText(), respCpf.getText(), respTelefone.getText()};
    }
}
