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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.EnumMap;
import java.util.Map;

/**
 * Clube Geeks: busca um cliente JÁ cadastrado e permite assinar/gerenciar o plano.
 * O cadastro de novos clientes fica somente na tela Cadastros.
 */
public class PainelClube extends PainelPagina {
    private final CampoGeeks campoBusca = new CampoGeeks("Nome ou CPF do cliente");
    private final BotaoGeeks btBuscar = new BotaoGeeks("Buscar", Icone.Tipo.LUPA, BotaoGeeks.Estilo.PREENCHIDO);
    private final BotaoGeeks btGerenciar = new BotaoGeeks("Gerenciar assinatura", Icone.Tipo.LISTA, BotaoGeeks.Estilo.CONTORNO);
    private final JPanel areaCliente = Componentes.transparente(new BorderLayout());
    private final Map<Plano, BotaoGeeks> botoesPlano = new EnumMap<>(Plano.class);
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
        direita.add(Componentes.rotuloIcone("Escolha a assinatura", Icone.Tipo.ESTRELA, Tema.AMARELO, 22), BorderLayout.NORTH);
        JPanel planos = Componentes.transparente(new GridLayout(1, 2, 16, 0));
        planos.add(criarCartaoPlano(Plano.BASICO, false));
        planos.add(criarCartaoPlano(Plano.PREMIUM, true));
        JPanel blocoPlanos = Componentes.transparente(new BorderLayout(0, 16));
        blocoPlanos.add(planos, BorderLayout.CENTER);
        blocoPlanos.add(btGerenciar, BorderLayout.SOUTH);
        JPanel envolvePlanos = Componentes.transparente(new BorderLayout());
        envolvePlanos.add(blocoPlanos, BorderLayout.NORTH);
        direita.add(envolvePlanos, BorderLayout.CENTER);

        corpo.add(esquerda);
        corpo.add(direita);
        getCorpo().add(corpo, BorderLayout.CENTER);
        limparSelecao();
    }

    private JComponent criarCartaoPlano(Plano plano, boolean destaque) {
        Color fundo = destaque ? Tema.AMARELO : Tema.CARTAO;
        Color texto = destaque ? Tema.FUNDO : Tema.BRANCO;
        CartaoGeeks c = new CartaoGeeks(fundo, destaque ? Tema.AMARELO : Tema.BORDA, false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(new EmptyBorder(12, 16, 16, 16));
        if (destaque) {
            Componentes.Faixa faixa = new Componentes.Faixa();
            faixa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
            faixa.setAlignmentX(CENTER_ALIGNMENT);
            c.add(faixa);
            c.add(Box.createVerticalStrut(8));
        }
        if (!destaque) {
            c.add(Box.createVerticalStrut(18));
        }
        Componentes.BlocoIcone bloco = new Componentes.BlocoIcone(destaque ? Icone.Tipo.COROA : Icone.Tipo.ESTRELA,
                destaque ? Tema.FUNDO : Tema.AMARELO, destaque ? Tema.AMARELO : Tema.FUNDO, 60, true);
        bloco.setAlignmentX(CENTER_ALIGNMENT);
        bloco.setMaximumSize(new Dimension(60, 60));
        c.add(bloco);
        c.add(Box.createVerticalStrut(8));
        c.add(centralizar(Componentes.rotulo("Plano " + plano.getNome(), Font.BOLD, 22, texto)));
        c.add(centralizar(Componentes.rotulo(Formatador.moeda(plano.getMensalidade()) + "/mês",
                Font.BOLD | Font.ITALIC, 26, texto)));
        c.add(Box.createVerticalStrut(10));
        JPanel beneficios = Componentes.transparente(new GridLayout(0, 1, 0, 6));
        for (String b : plano.getBeneficios()) {
            JLabel l = Componentes.rotulo(b, Font.PLAIN, 15, texto);
            l.setIcon(new Icone(Icone.Tipo.CHECK, 16, texto));
            l.setIconTextGap(8);
            beneficios.add(l);
        }
        beneficios.setAlignmentX(CENTER_ALIGNMENT);
        beneficios.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        c.add(beneficios);
        c.add(Box.createVerticalStrut(18));
        BotaoGeeks assinar = new BotaoGeeks("Assinar", null, destaque ? BotaoGeeks.Estilo.ESCURO : BotaoGeeks.Estilo.CONTORNO);
        assinar.setActionCommand(plano.name());
        assinar.setAlignmentX(CENTER_ALIGNMENT);
        assinar.setMaximumSize(new Dimension(Integer.MAX_VALUE, assinar.getPreferredSize().height));
        botoesPlano.put(plano, assinar);
        c.add(assinar);
        return c;
    }

    private JComponent centralizar(JComponent c) {
        c.setAlignmentX(CENTER_ALIGNMENT);
        return c;
    }

    public void limparSelecao() {
        areaCliente.removeAll();
        areaCliente.add(Componentes.rotulo("<html>Busque um cliente já cadastrado para ver ou alterar a assinatura.</html>",
                Font.PLAIN, 16, Tema.CINZA), BorderLayout.NORTH);
        atualizarPlanos(null);
        refrescar();
    }

    public void mostrarCliente(Cliente c) {
        areaCliente.removeAll();
        JPanel info = Componentes.transparente(new BorderLayout(14, 0));
        info.add(new Componentes.BlocoIcone(Icone.Tipo.USUARIO_MAIS, Tema.AMARELO, Tema.FUNDO, 60, true), BorderLayout.WEST);
        JPanel textos = Componentes.transparente(new GridLayout(3, 1, 0, 2));
        textos.add(Componentes.rotulo(c.getNome(), Font.BOLD, 20, Tema.BRANCO));
        textos.add(Componentes.rotulo("CPF " + Formatador.cpf(c.getCpf()) + "  |  " + c.getTelefone(), Font.PLAIN, 14, Tema.CINZA));
        JPanel linhaChip = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 2));
        linhaChip.add(new Componentes.Chip(c.getPlano() == Plano.NENHUM ? "Sem plano" : "Plano " + c.getPlano().getNome(),
                c.getPlano() == Plano.NENHUM ? Tema.CINZA : Tema.AMARELO, Icone.Tipo.COROA));
        textos.add(linhaChip);
        info.add(textos, BorderLayout.CENTER);
        areaCliente.add(info, BorderLayout.NORTH);
        atualizarPlanos(c);
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
        atualizarPlanos(null);
        refrescar();
    }

    private void atualizarPlanos(Cliente c) {
        for (Map.Entry<Plano, BotaoGeeks> e : botoesPlano.entrySet()) {
            boolean atual = c != null && c.getPlano() == e.getKey();
            e.getValue().setText(atual ? "Plano atual" : "Assinar");
            e.getValue().setEnabled(c != null && !atual);
        }
        btGerenciar.setEnabled(c != null && c.getPlano() != Plano.NENHUM);
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

    /** O actionCommand do evento é o nome do Plano (BASICO ou PREMIUM). */
    public void addListenerAssinar(ActionListener l) {
        botoesPlano.values().forEach(b -> b.addActionListener(l));
    }

    public void addListenerGerenciar(ActionListener l) {
        btGerenciar.addActionListener(l);
    }

    public void addListenerNovoCliente(ActionListener l) {
        this.listenerNovoCliente = l;
    }
}
