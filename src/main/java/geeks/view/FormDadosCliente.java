package geeks.view;

import geeks.model.Cliente;
import geeks.model.FichaCliente;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

/**
 * Ficha do cliente (janela "Dados do usuário"): dados pessoais, responsável, Clube e uso.
 * Sem altura fixa: a janela cresce com o conteúdo (e rola em telas baixas).
 */
public class FormDadosCliente extends FormularioGeeks {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Componentes.BlocoIcone avatar;

    public FormDadosCliente(FichaCliente f) {
        super("Dados do usuário", "Editar dados", Icone.Tipo.LAPIS, true);
        setTextoCancelar("Fechar");
        Cliente c = f.getCliente();
        avatar = new Componentes.BlocoIcone(Icone.Tipo.USUARIO_MAIS, Tema.AMARELO, Tema.FUNDO, 72, true).comMargem(8);

        JPanel topo = Componentes.transparente(new BorderLayout(10, 0));
        topo.add(avatar, BorderLayout.WEST);
        JPanel nomes = Componentes.transparente(new GridLayout(2, 1, 0, 6));
        nomes.add(Componentes.rotulo(c.getNome(), Font.BOLD, 24, Tema.BRANCO));
        JPanel chips = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.add(new Componentes.Chip(f.getClubeAte() != null ? "Clube Geeks até " + f.getClubeAte().format(DATA) : "Sem plano",
                f.getClubeAte() != null ? Tema.AMARELO : Tema.CINZA, Icone.Tipo.COROA));
        if (c.isMenor()) {
            chips.add(new Componentes.Chip("Menor de idade", Tema.AZUL, Icone.Tipo.INFO));
        }
        nomes.add(chips);
        topo.add(nomes, BorderLayout.CENTER);

        JPanel secoes = new JPanel() {
            @Override
            public java.awt.Component add(java.awt.Component comp) {
                if (getComponentCount() > 0) {
                    super.add(javax.swing.Box.createVerticalStrut(14));
                }
                ((JComponent) comp).setAlignmentX(LEFT_ALIGNMENT);
                return super.add(comp);
            }
        };
        secoes.setOpaque(false);
        secoes.setLayout(new javax.swing.BoxLayout(secoes, javax.swing.BoxLayout.Y_AXIS));
        secoes.add(secao("Dados pessoais", Icone.Tipo.USUARIO_MAIS,
                "Nascimento", c.getDataNascimento() == null ? "-" : c.getDataNascimento().format(DATA) + "  (" + c.getIdade() + " anos)",
                "Telefone", vazio(c.getTelefone()),
                "CPF", vazio(Formatador.cpf(c.getCpf())),
                "Cliente desde", f.getCadastro() == null ? "-" : f.getCadastro().format(DATA)));
        if (f.temResponsavel()) {
            secoes.add(secao("Responsável", Icone.Tipo.USUARIO_MAIS,
                    "Nome", f.getRespNome() + (vazio(f.getRespParentesco()).equals("-") ? "" : "  (" + f.getRespParentesco() + ")"),
                    "Telefone", vazio(f.getRespTelefone()),
                    "CPF", vazio(Formatador.cpf(f.getRespCpf())),
                    null, null));
        }
        secoes.add(secao("Uso na Geeks", Icone.Tipo.CONTROLE,
                "Sessões", String.valueOf(f.getSessoes()),
                "Última visita", f.getUltimaVisita() == null ? "-" : f.getUltimaVisita().format(DATA),
                "Crédito de tempo", f.getSaldoCreditoMinutos() > 0 ? f.getSaldoCreditoMinutos() + " min" : "nenhum",
                null, null));

        CartaoGeeks cartao = new CartaoGeeks(Tema.CAMPO, new Color(0x333333), false);
        cartao.setLayout(new BorderLayout(0, 18));
        cartao.setBorder(new EmptyBorder(16, 20, 18, 20));
        cartao.add(topo, BorderLayout.NORTH);
        cartao.add(secoes, BorderLayout.CENTER);
        JPanel largura = Componentes.transparente(new BorderLayout());
        largura.add(cartao, BorderLayout.CENTER);
        largura.setPreferredSize(new java.awt.Dimension(560, largura.getPreferredSize().height));
        getCorpo().add(largura, BorderLayout.CENTER);
    }

    /** Uma seção com título e até 4 pares rótulo/valor (pares null são ignorados). */
    private JComponent secao(String titulo, Icone.Tipo icone, String... pares) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 8));
        p.add(Componentes.rotuloIcone(titulo, icone, Tema.AMARELO, 15), BorderLayout.NORTH);
        JPanel dados = Componentes.transparente(new GridLayout(0, 2, 12, 6));
        for (int i = 0; i + 1 < pares.length; i += 2) {
            if (pares[i] == null) {
                continue;
            }
            dados.add(Componentes.rotulo(pares[i], Font.PLAIN, 15, Tema.CINZA));
            dados.add(Componentes.rotulo(pares[i + 1], Font.BOLD, 15, Tema.BRANCO));
        }
        p.add(dados, BorderLayout.CENTER);
        return p;
    }

    private static String vazio(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    @Override
    public void animarEntrada() {
        super.animarEntrada();
        avatar.pop(100);
    }
}
