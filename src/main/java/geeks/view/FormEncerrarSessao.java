package geeks.view;

import geeks.model.Sessao;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Confirmação de encerramento: mostra quem, onde, quanto tempo e o total (que "conta" até o valor final). */
public class FormEncerrarSessao extends FormularioGeeks {
    private final JLabel lbTotal = Componentes.rotulo("R$ 0,00", Font.BOLD | Font.ITALIC, 38, Tema.AMARELO);
    private final BigDecimal total;

    public FormEncerrarSessao(Sessao s) {
        super("Encerrar sessão", "Encerrar sessão", Icone.Tipo.RAIO);
        this.total = s.getValor();

        CartaoGeeks cartao = new CartaoGeeks(Tema.CAMPO, new Color(0x333333), false);
        cartao.setLayout(new BorderLayout(0, 18));
        cartao.setBorder(new EmptyBorder(18, 20, 18, 20));
        cartao.setPreferredSize(new Dimension(520, 200));

        JPanel topo = Componentes.transparente(new BorderLayout(14, 0));
        topo.add(new Componentes.BlocoIcone(s.getDispositivo().getTipo(), 56), BorderLayout.WEST);
        JPanel nomes = Componentes.transparente(new GridLayout(2, 1, 0, 6));
        nomes.add(Componentes.rotulo(s.getCliente().getNome(), Font.BOLD, 22, Tema.BRANCO));
        JPanel linhaChip = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaChip.add(new Componentes.Chip(s.getDispositivo().getNome(), Tema.corDe(s.getDispositivo().getTipo()),
                Tema.iconeDe(s.getDispositivo().getTipo())));
        nomes.add(linhaChip);
        topo.add(nomes, BorderLayout.CENTER);

        JPanel estatisticas = Componentes.transparente(new GridLayout(1, 2, 16, 0));
        estatisticas.add(estatistica("Tempo de uso", Componentes.rotulo(Formatador.duracao(s.getDuracao()),
                Font.BOLD | Font.ITALIC, 38, Tema.BRANCO)));
        estatisticas.add(estatistica("Total a cobrar", lbTotal));

        cartao.add(topo, BorderLayout.NORTH);
        cartao.add(estatisticas, BorderLayout.CENTER);
        getCorpo().add(cartao, BorderLayout.CENTER);
        setResumo("Ao confirmar, a sessão é finalizada e vai para o histórico.");
    }

    private JPanel estatistica(String titulo, JLabel valor) {
        JPanel p = Componentes.transparente(new BorderLayout(0, 2));
        p.add(Componentes.rotulo(titulo, Font.PLAIN, 14, Tema.CINZA), BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void animarEntrada() {
        super.animarEntrada();
        Animacao.executar(800, 180, p -> {
            BigDecimal v = total.multiply(BigDecimal.valueOf(Animacao.suave(p))).setScale(2, RoundingMode.HALF_UP);
            lbTotal.setText(Formatador.moeda(v));
        }, () -> lbTotal.setText(Formatador.moeda(total)));
    }
}
