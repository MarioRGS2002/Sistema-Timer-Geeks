package geeks.view;

import geeks.model.LojaModel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loja (só gerente): preços (valor da hora de cada tipo e mensalidade do Clube) e equipamentos
 * (cadastrar, colocar em manutenção, voltar a usar).
 */
public class PainelLoja extends PainelPagina {
    private final Map<String, CampoGeeks> camposPreco = new LinkedHashMap<>();
    private final JPanel listaPrecos = Componentes.transparente(new GridLayout(0, 2, 12, 10));
    private final BotaoGeeks btSalvarPrecos = new BotaoGeeks("Salvar preços", Icone.Tipo.CHECK, BotaoGeeks.Estilo.PREENCHIDO);
    private final CampoGeeks campoEquip = new CampoGeeks("Nome (ex.: PC05)");
    private final JPanel areaTipos = Componentes.transparente(new BorderLayout());
    private SeletorOpcoes tipos;
    private final BotaoGeeks btAdicionar = new BotaoGeeks("Adicionar", Icone.Tipo.MAIS, BotaoGeeks.Estilo.ESCURO);
    private final JPanel listaEquip = Componentes.transparente(new GridLayout(0, 1, 0, 8));
    private ActionListener listenerManutencao;

    public PainelLoja() {
        super("Loja");
        JPanel corpo = Componentes.transparente(new GridLayout(1, 2, 26, 0));

        CartaoGeeks precos = new CartaoGeeks();
        precos.setLayout(new BorderLayout(0, 14));
        precos.setBorder(new EmptyBorder(20, 22, 20, 22));
        precos.add(Componentes.rotuloIcone("Preços", Icone.Tipo.MOEDA, Tema.BRANCO, 22), BorderLayout.NORTH);
        JPanel meioPrecos = Componentes.transparente(new BorderLayout(0, 12));
        meioPrecos.add(listaPrecos, BorderLayout.NORTH);
        meioPrecos.add(Componentes.rotulo("<html>O novo preço vale para as sessões que começarem depois de salvar; "
                + "as que já estão abertas continuam com o preço de quando começaram.</html>", Font.PLAIN, 13, Tema.CINZA),
                BorderLayout.CENTER);
        precos.add(meioPrecos, BorderLayout.CENTER);
        JPanel rodape = Componentes.transparente(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rodape.add(btSalvarPrecos);
        precos.add(rodape, BorderLayout.SOUTH);

        CartaoGeeks equip = new CartaoGeeks();
        equip.setLayout(new BorderLayout(0, 14));
        equip.setBorder(new EmptyBorder(20, 22, 20, 22));
        JPanel novo = Componentes.transparente(new BorderLayout(0, 10));
        novo.add(Componentes.rotuloIcone("Equipamentos", Icone.Tipo.CONTROLE, Tema.BRANCO, 22), BorderLayout.NORTH);
        JPanel linha = Componentes.transparente(new BorderLayout(10, 0));
        linha.add(campoEquip, BorderLayout.CENTER);
        linha.add(btAdicionar, BorderLayout.EAST);
        JPanel cadastro = Componentes.transparente(new BorderLayout(0, 8));
        cadastro.add(linha, BorderLayout.NORTH);
        cadastro.add(areaTipos, BorderLayout.CENTER);
        novo.add(cadastro, BorderLayout.CENTER);
        equip.add(novo, BorderLayout.NORTH);
        JPanel envolveLista = Componentes.transparente(new BorderLayout());
        envolveLista.add(listaEquip, BorderLayout.NORTH);
        equip.add(rolagem(envolveLista), BorderLayout.CENTER);
        campoEquip.addActionListener(e -> btAdicionar.doClick());

        corpo.add(precos);
        corpo.add(equip);
        getCorpo().add(corpo, BorderLayout.CENTER);
    }

    public void atualizarPrecos(Map<String, BigDecimal> precos) {
        listaPrecos.removeAll();
        camposPreco.clear();
        for (Map.Entry<String, BigDecimal> e : precos.entrySet()) {
            boolean clube = e.getKey().toLowerCase().contains("clube");
            listaPrecos.add(Componentes.rotuloIcone(clube ? e.getKey() + " (por mês)" : e.getKey() + " (por hora)",
                    clube ? Icone.Tipo.COROA : Icone.Tipo.RELOGIO, Tema.BRANCO, 15));
            CampoGeeks campo = new CampoGeeks("0,00");
            campo.setText(e.getValue().toPlainString().replace('.', ','));
            camposPreco.put(e.getKey(), campo);
            listaPrecos.add(campo);
        }
        listaPrecos.revalidate();
        listaPrecos.repaint();
    }

    public void atualizarEquipamentos(List<LojaModel.Equipamento> lista, List<String> nomesTipos) {
        if (tipos == null && !nomesTipos.isEmpty()) {
            tipos = new SeletorOpcoes(nomesTipos, 0);
            areaTipos.add(tipos, BorderLayout.WEST);
        }
        listaEquip.removeAll();
        if (lista.isEmpty()) {
            listaEquip.add(Componentes.rotulo("Nenhum equipamento cadastrado.", Font.PLAIN, 16, Tema.CINZA));
        }
        for (LojaModel.Equipamento e : lista) {
            listaEquip.add(linhaEquipamento(e));
        }
        listaEquip.revalidate();
        listaEquip.repaint();
    }

    private JComponent linhaEquipamento(LojaModel.Equipamento e) {
        CartaoGeeks c = new CartaoGeeks();
        c.setLayout(new BorderLayout(12, 0));
        c.setBorder(new EmptyBorder(8, 14, 8, 10));
        JPanel textos = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 10, 4));
        textos.add(Componentes.rotulo(e.nome(), Font.BOLD, 16, e.emManutencao() ? Tema.CINZA : Tema.BRANCO));
        textos.add(new Componentes.Chip(e.tipo(), Tema.AZUL, Icone.Tipo.CONTROLE));
        textos.add(new Componentes.Chip(e.emManutencao() ? "Em manutenção" : e.emUso() ? "Em uso" : "Disponível",
                e.emManutencao() ? Tema.VERMELHO : e.emUso() ? Tema.AMARELO : Tema.CINZA,
                e.emManutencao() ? Icone.Tipo.ALERTA : Icone.Tipo.CHECK));
        c.add(textos, BorderLayout.CENTER);
        BotaoGeeks acao = new BotaoGeeks(e.emManutencao() ? "Voltar a usar" : "Manutenção", null,
                e.emManutencao() ? BotaoGeeks.Estilo.ESCURO : BotaoGeeks.Estilo.CONTORNO);
        acao.setActionCommand(e.id() + ":" + (e.emManutencao() ? "voltar" : "manutencao") + ":" + e.nome());
        acao.setEnabled(!e.emUso());
        if (e.emUso()) {
            acao.setToolTipText("Em uso agora: encerre a sessão antes.");
        }
        if (listenerManutencao != null) {
            acao.addActionListener(listenerManutencao);
        }
        c.add(acao, BorderLayout.EAST);
        return c;
    }

    /** Preços digitados (texto, como "10,50"), na mesma ordem da tela. */
    public Map<String, String> getPrecosDigitados() {
        Map<String, String> m = new LinkedHashMap<>();
        camposPreco.forEach((k, v) -> m.put(k, v.getText()));
        return m;
    }

    public String getNomeEquipamento() {
        return campoEquip.getText();
    }

    public String getTipoEquipamento() {
        return tipos == null ? null : tipos.getSelecionada();
    }

    public void limparNovoEquipamento() {
        campoEquip.setText("");
    }

    public void addListenerSalvarPrecos(ActionListener l) {
        btSalvarPrecos.addActionListener(l);
    }

    public void addListenerAdicionar(ActionListener l) {
        btAdicionar.addActionListener(l);
    }

    /** O actionCommand é "id:manutencao:nome" ou "id:voltar:nome". */
    public void addListenerManutencao(ActionListener l) {
        this.listenerManutencao = l;
    }
}
