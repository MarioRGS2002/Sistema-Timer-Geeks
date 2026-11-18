package geeks.view;

import geeks.model.Cliente;
import geeks.model.Plano;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

/**
 * Clube Geeks: busca um cliente JÁ cadastrado e permite assinar ou cancelar o plano único do clube.
 * O cadastro de novos clientes fica somente na tela Cadastros.
 */
public class PainelClube extends PainelPagina {
    private final CampoGeeks campoBusca = new CampoGeeks("Nome ou CPF do cliente");
    private final BotaoGeeks btBuscar = new BotaoGeeks("Buscar", Icone.Tipo.LUPA, BotaoGeeks.Estilo.PREENCHIDO);
    private final BotaoGeeks btAssinar = new BotaoGeeks("Assinar", null, BotaoGeeks.Estilo.ESCURO);
    private final BotaoGeeks btCancelar = new BotaoGeeks("Cancelar assinatura", Icone.Tipo.LISTA, BotaoGeeks.Estilo.CONTORNO);
    private final JPanel areaCliente = Componentes.transparente(new BorderLayout());
    private ActionListener listenerNovoCliente;

    public PainelClube() {
        super("Clube Geeks");
        JPanel corpo = Componentes.transparente(new GridLayout(1, 2, 26, 0));

        CartaoGeeks esquerda = new CartaoGeeks();
        esquerda.setLayout(new BorderLayout(0, 18));
        esquerda.setBorder(new EmptyBorder(22, 22, 22, 22));
        JPanel busca = Componentes.transparente(new GridLayout(0, 1, 0, 8));
        busca.add(Componentes.rotuloIcone("Cliente do clube", Icone.Tipo.USUARIO_MAIS, Tema.BRANCO, 22));
        busca.add(Componentes.rotulo("Buscar por nome ou CPF", Font.BOLD, 15, Tema.CINZA));
        JPanel linhaBusca = Componentes.transparente(new BorderLayout(10, 0));
        linhaBusca.add(campoBusca, BorderLayout.CENTER);
        linhaBusca.add(btBuscar, BorderLayout.EAST);
        busca.add(linhaBusca);
        JPanel envoltorio = Componentes.transparente(new BorderLayout());
        envoltorio.add(areaCliente, BorderLayout.NORTH);
        esquerda.add(busca, BorderLayout.NORTH);
        esquerda.add(envoltorio, BorderLayout.CENTER);

        JPanel direita = Componentes.transparente(new BorderLayout(0, 16));
        direita.add(Componentes.rotuloIcone("Assinatura do clube", Icone.Tipo.ESTRELA, Tema.AMARELO, 22), BorderLayout.NORTH);
        JPanel bloco = Componentes.transparente(new BorderLayout(0, 16));
        bloco.add(criarCartaoPlano(Plano.CLUBE), BorderLayout.CENTER);
        bloco.add(btCancelar, BorderLayout.SOUTH);
        JPanel envolvePlano = Componentes.transparente(new BorderLayout());
        envolvePlano.add(bloco, BorderLayout.NORTH);
        direita.add(envolvePlano, BorderLayout.CENTER);

        corpo.add(esquerda);
        corpo.add(direita);
        getCorpo().add(corpo, BorderLayout.CENTER);
        btAssinar.setActionCommand(Plano.CLUBE.name());
        limparSelecao();
    }

    private JComponent criarCartaoPlano(Plano plano) {
        CartaoGeeks c = new CartaoGeeks(Tema.AMARELO, Tema.AMARELO, false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(new EmptyBorder(14, 22, 20, 22));
        Componentes.Faixa faixa = new Componentes.Faixa();
        faixa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        faixa.setAlignmentX(CENTER_ALIGNMENT);
        c.add(faixa);
        c.add(Box.createVerticalStrut(14));
        Componentes.BlocoIcone bloco = new Componentes.BlocoIcone(Icone.Tipo.COROA, Tema.FUNDO, Tema.AMARELO, 64, true);
        bloco.setAlignmentX(CENTER_ALIGNMENT);
        bloco.setMaximumSize(new Dimension(64, 64));
        c.add(bloco);
        c.add(Box.createVerticalStrut(8));
        c.add(centralizar(Componentes.rotulo(plano.getNome(), Font.BOLD | Font.ITALIC, 30, Tema.FUNDO)));
        c.add(centralizar(Componentes.rotulo(Formatador.moeda(plano.getValor()), Font.BOLD | Font.ITALIC, 40, Tema.FUNDO)));
        c.add(Box.createVerticalStrut(12));
        JPanel beneficios = Componentes.transparente(new GridLayout(0, 1, 0, 6));
        for (String b : plano.getBeneficios()) {
            JLabel l = Componentes.rotulo(b, Font.PLAIN, 16, Tema.FUNDO);
            l.setIcon(new Icone(Icone.Tipo.CHECK, 16, Tema.FUNDO));
            l.setIconTextGap(8);
            beneficios.add(l);
        }
        beneficios.setAlignmentX(CENTER_ALIGNMENT);
        beneficios.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        c.add(beneficios);
        c.add(Box.createVerticalStrut(18));
        btAssinar.setAlignmentX(CENTER_ALIGNMENT);
        btAssinar.setMaximumSize(new Dimension(Integer.MAX_VALUE, btAssinar.getPreferredSize().height));
        c.add(btAssinar);
        return c;
    }

    private JComponent centralizar(JComponent c) {
        c.setAlignmentX(CENTER_ALIGNMENT);
        return c;
    }

    public void limparSelecao() {
        areaCliente.removeAll();
        areaCliente.add(Componentes.rotulo("<html>Busque um cliente já cadastrado para assinar ou cancelar o plano.</html>",
                Font.PLAIN, 16, Tema.CINZA), BorderLayout.NORTH);
        atualizarPlano(null);
        refrescar();
    }

    public void mostrarCliente(Cliente c) {
        areaCliente.removeAll();
        boolean assinante = c.getPlano() != Plano.NENHUM;
        JPanel info = Componentes.transparente(new BorderLayout(14, 0));
        info.add(new Componentes.BlocoIcone(Icone.Tipo.USUARIO_MAIS, Tema.AMARELO, Tema.FUNDO, 60, true), BorderLayout.WEST);
        JPanel textos = Componentes.transparente(new GridLayout(3, 1, 0, 2));
        textos.add(Componentes.rotulo(c.getNome(), Font.BOLD, 20, Tema.BRANCO));
        textos.add(Componentes.rotulo("CPF " + Formatador.cpf(c.getCpf()) + "  |  " + c.getTelefone(), Font.PLAIN, 14, Tema.CINZA));
        JPanel linhaChip = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 2));
        linhaChip.add(new Componentes.Chip(assinante ? "Assinante Clube Geeks" : "Sem plano",
                assinante ? Tema.AMARELO : Tema.CINZA, Icone.Tipo.COROA));
        textos.add(linhaChip);
        info.add(textos, BorderLayout.CENTER);
        areaCliente.add(info, BorderLayout.NORTH);
        atualizarPlano(c);
        refrescar();
    }

    public void mostrarNaoEncontrado(String termo) {
        areaCliente.removeAll();
        JPanel p = Componentes.transparente(new GridLayout(0, 1, 0, 12));
        p.add(Componentes.rotulo("<html>Nenhum cliente encontrado para \u201C" + termo + "\u201D.</html>", Font.PLAIN, 16, Tema.CINZA));
        JPanel linha = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        BotaoGeeks novo = new BotaoGeeks("Cadastrar novo cliente", Icone.Tipo.USUARIO_MAIS, BotaoGeeks.Estilo.CONTORNO);
        if (listenerNovoCliente != null) {
            novo.addActionListener(listenerNovoCliente);
        }
        linha.add(novo);
        p.add(linha);
        areaCliente.add(p, BorderLayout.NORTH);
        atualizarPlano(null);
        refrescar();
    }

    private void atualizarPlano(Cliente c) {
        boolean assinante = c != null && c.getPlano() != Plano.NENHUM;
        btAssinar.setText(assinante ? "Plano ativo" : "Assinar");
        btAssinar.setEnabled(c != null && !assinante);
        btCancelar.setEnabled(assinante);
    }

    private void refrescar() {
        areaCliente.revalidate();
        areaCliente.repaint();
    }

    public String getTermoBusca() {
        return campoBusca.getText();
    }

    public void addListenerBuscar(ActionListener l) {
        btBuscar.addActionListener(l);
        campoBusca.addActionListener(l);
    }

    /** O actionCommand do evento é o nome do Plano (CLUBE). */
    public void addListenerAssinar(ActionListener l) {
        btAssinar.addActionListener(l);
    }

    public void addListenerCancelar(ActionListener l) {
        btCancelar.addActionListener(l);
    }

    public void addListenerNovoCliente(ActionListener l) {
        this.listenerNovoCliente = l;
    }
}
