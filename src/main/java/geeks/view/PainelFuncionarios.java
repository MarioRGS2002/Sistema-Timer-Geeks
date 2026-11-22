package geeks.view;

import geeks.model.Funcionario;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Tela Funcionários (visível só para o gerente): quem pode entrar no sistema.
 * O gerente cadastra, troca a senha, desativa e reativa. Nada é apagado: desativado não entra mais,
 * mas continua no histórico.
 */
public class PainelFuncionarios extends PainelPagina {
    private final JPanel lista = Componentes.transparente(new GridLayout(0, 1, 0, 12));
    private final Componentes.Pilula pilulaAtivos = new Componentes.Pilula(Icone.Tipo.USUARIO_MAIS, "0 ativos");
    private final BotaoGeeks btNovo = new BotaoGeeks("Novo funcionário", Icone.Tipo.MAIS, BotaoGeeks.Estilo.PREENCHIDO);
    private ActionListener listenerSenha;
    private ActionListener listenerDesativar;
    private ActionListener listenerReativar;

    public PainelFuncionarios() {
        super("Funcionários");
        JPanel direita = Componentes.transparente(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        direita.add(pilulaAtivos);
        direita.add(btNovo);
        setLadoDireito(direita);

        JPanel aviso = Componentes.transparente(new BorderLayout());
        aviso.setBorder(new EmptyBorder(0, 0, 18, 0));
        aviso.add(Componentes.rotulo("<html>Só quem está ativo consegue entrar no sistema. Gerentes também veem o faturamento "
                + "e esta tela. Desativar não apaga: o histórico continua com o nome do funcionário.</html>",
                Font.PLAIN, 15, Tema.CINZA), BorderLayout.CENTER);

        JPanel envoltorio = Componentes.transparente(new BorderLayout());
        envoltorio.add(lista, BorderLayout.NORTH);
        getCorpo().add(aviso, BorderLayout.NORTH);
        getCorpo().add(rolagem(envoltorio), BorderLayout.CENTER);
    }

    /** @param idLogado id do gerente logado (ele não pode desativar a si mesmo) */
    public void atualizar(List<Funcionario> funcionarios, int idLogado) {
        long ativos = funcionarios.stream().filter(Funcionario::isAtivo).count();
        pilulaAtivos.setText(ativos + (ativos == 1 ? " ativo" : " ativos"));
        lista.removeAll();
        if (funcionarios.isEmpty()) {
            lista.add(Componentes.rotulo("Nenhum funcionário cadastrado.", Font.PLAIN, 18, Tema.CINZA));
        }
        for (Funcionario f : funcionarios) {
            lista.add(criarLinha(f, f.getId() == idLogado));
        }
        lista.revalidate();
        lista.repaint();
    }

    private JComponent criarLinha(Funcionario f, boolean euMesmo) {
        CartaoGeeks linha = new CartaoGeeks();
        linha.setLayout(new BorderLayout(16, 0));
        linha.setBorder(new EmptyBorder(12, 18, 12, 18));
        linha.add(new Componentes.BlocoIcone(f.isGerente() ? Icone.Tipo.COROA : Icone.Tipo.USUARIO_MAIS,
                f.isAtivo() ? Tema.AMARELO : Tema.BORDA, Tema.FUNDO, 48, true), BorderLayout.WEST);

        JPanel centro = Componentes.transparente(new GridLayout(1, 2, 12, 0));
        JPanel quem = Componentes.transparente(new GridLayout(2, 1, 0, 4));
        quem.add(Componentes.rotulo(f.getNome() + (euMesmo ? "  (você)" : ""), Font.BOLD, 18,
                f.isAtivo() ? Tema.BRANCO : Tema.CINZA));
        quem.add(Componentes.rotuloIcone("Login: " + f.getLogin(), Icone.Tipo.CADEADO, Tema.CINZA, 14));
        JPanel chips = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 8, 10));
        chips.add(new Componentes.Chip(f.isGerente() ? "Gerente" : "Atendente",
                f.isGerente() ? Tema.AMARELO : Tema.AZUL, f.isGerente() ? Icone.Tipo.COROA : Icone.Tipo.USUARIO_MAIS));
        chips.add(new Componentes.Chip(f.isAtivo() ? "Ativo" : "Desativado",
                f.isAtivo() ? Tema.CINZA : Tema.VERMELHO, f.isAtivo() ? Icone.Tipo.CHECK : Icone.Tipo.ALERTA));
        centro.add(quem);
        centro.add(chips);
        linha.add(centro, BorderLayout.CENTER);

        JPanel acoes = Componentes.transparente(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        BotaoGeeks senha = new BotaoGeeks("Trocar senha", Icone.Tipo.LAPIS, BotaoGeeks.Estilo.LINK);
        senha.setActionCommand(String.valueOf(f.getId()));
        if (listenerSenha != null) {
            senha.addActionListener(listenerSenha);
        }
        acoes.add(senha);
        if (f.isAtivo()) {
            BotaoGeeks desativar = new BotaoGeeks("Desativar", null, BotaoGeeks.Estilo.CONTORNO);
            desativar.setActionCommand(String.valueOf(f.getId()));
            desativar.setEnabled(!euMesmo);
            if (euMesmo) {
                desativar.setToolTipText("Você não pode desativar o seu próprio acesso.");
            } else if (listenerDesativar != null) {
                desativar.addActionListener(listenerDesativar);
            }
            acoes.add(desativar);
        } else {
            BotaoGeeks reativar = new BotaoGeeks("Reativar", Icone.Tipo.CHECK, BotaoGeeks.Estilo.ESCURO);
            reativar.setActionCommand(String.valueOf(f.getId()));
            if (listenerReativar != null) {
                reativar.addActionListener(listenerReativar);
            }
            acoes.add(reativar);
        }
        linha.add(acoes, BorderLayout.EAST);
        linha.ativarHover();
        return linha;
    }

    public void addListenerNovo(ActionListener l) {
        btNovo.addActionListener(l);
    }

    /** O actionCommand do evento é o id do funcionário. */
    public void addListenerTrocarSenha(ActionListener l) {
        this.listenerSenha = l;
    }

    /** O actionCommand do evento é o id do funcionário. */
    public void addListenerDesativar(ActionListener l) {
        this.listenerDesativar = l;
    }

    /** O actionCommand do evento é o id do funcionário. */
    public void addListenerReativar(ActionListener l) {
        this.listenerReativar = l;
    }
}
