package geeks.view;

import geeks.model.Cliente;
import geeks.model.Plano;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

/** Ficha do cliente (janela "Dados do usuário"). */
public class FormDadosCliente extends FormularioGeeks {
    private final Componentes.BlocoIcone avatar;

    public FormDadosCliente(Cliente c) {
        super("Dados do usuário", "Fechar", Icone.Tipo.CHECK, false);
        avatar = new Componentes.BlocoIcone(Icone.Tipo.USUARIO_MAIS, Tema.AMARELO, Tema.FUNDO, 72, true).comMargem(8);

        CartaoGeeks cartao = new CartaoGeeks(Tema.CAMPO, new Color(0x333333), false);
        cartao.setLayout(new BorderLayout(0, 16));
        cartao.setBorder(new EmptyBorder(16, 20, 18, 20));
        cartao.setPreferredSize(new Dimension(520, c.getResponsavel().isEmpty() ? 215 : 255));

        JPanel topo = Componentes.transparente(new BorderLayout(10, 0));
        topo.add(avatar, BorderLayout.WEST);
        JPanel nomes = Componentes.transparente(new GridLayout(2, 1, 0, 6));
        nomes.add(Componentes.rotulo(c.getNome(), Font.BOLD, 24, Tema.BRANCO));
        JPanel linhaChip = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        boolean assinante = c.getPlano() != Plano.NENHUM;
        linhaChip.add(new Componentes.Chip(assinante ? "Assinante Clube Geeks" : "Sem plano",
                assinante ? Tema.AMARELO : Tema.CINZA, Icone.Tipo.COROA));
        nomes.add(linhaChip);
        topo.add(nomes, BorderLayout.CENTER);

        JPanel dados = Componentes.transparente(new GridLayout(0, 2, 12, 12));
        linha(dados, "Idade", c.getIdade() + " anos");
        linha(dados, "Telefone", c.getTelefone().isEmpty() ? "-" : c.getTelefone());
        linha(dados, "CPF", Formatador.cpf(c.getCpf()));
        if (!c.getResponsavel().isEmpty()) {
            linha(dados, "Responsável", c.getResponsavel());
        }
        JPanel envolve = Componentes.transparente(new BorderLayout());
        envolve.add(dados, BorderLayout.NORTH);

        cartao.add(topo, BorderLayout.NORTH);
        cartao.add(envolve, BorderLayout.CENTER);
        getCorpo().add(cartao, BorderLayout.CENTER);
    }

    private void linha(JPanel painel, String rotulo, String valor) {
        painel.add(Componentes.rotulo(rotulo, Font.PLAIN, 15, Tema.CINZA));
        painel.add(Componentes.rotulo(valor, Font.BOLD, 16, Tema.BRANCO));
    }

    @Override
    public void animarEntrada() {
        super.animarEntrada();
        avatar.pop(100);
    }
}
