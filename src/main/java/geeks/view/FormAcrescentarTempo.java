package geeks.view;

import geeks.model.Sessao;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** "Mais tempo" numa sessão de tempo fechado: quanto acrescentar, o novo horário de fim e o valor a mais. */
public class FormAcrescentarTempo extends FormularioGeeks {
    private static final int[] MINUTOS = {30, 60, 90, 120};
    private final SeletorOpcoes tempo = new SeletorOpcoes(List.of("30 min", "1 h", "1h30", "2 h"), 0);
    private final Sessao sessao;

    public FormAcrescentarTempo(Sessao s) {
        super("Mais tempo", "Acrescentar", Icone.Tipo.MAIS);
        this.sessao = s;
        JPanel tudo = Componentes.transparente(new BorderLayout(0, 16));
        tudo.add(Componentes.rotulo(s.getCliente().getNome() + " em " + s.getDispositivo().getNome()
                + "  ·  até " + s.getPrevisto().format(DateTimeFormatter.ofPattern("HH:mm")), Font.BOLD, 17, Tema.BRANCO),
                BorderLayout.NORTH);
        tudo.add(bloco("Acrescentar", Icone.Tipo.RELOGIO, tempo), BorderLayout.CENTER);
        tudo.setPreferredSize(new java.awt.Dimension(480, tudo.getPreferredSize().height));
        getCorpo().add(tudo, BorderLayout.CENTER);
        tempo.addListenerSelecao(this::atualizar);
        atualizar();
    }

    private void atualizar() {
        LocalDateTime base = sessao.getPrevisto().isAfter(LocalDateTime.now()) ? sessao.getPrevisto() : LocalDateTime.now();
        BigDecimal valor = sessao.getValorHora().multiply(BigDecimal.valueOf(getMinutos()))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        setResumo("Novo fim às " + base.plusMinutes(getMinutos()).format(DateTimeFormatter.ofPattern("HH:mm"))
                + "  ·  + " + Formatador.moeda(valor) + " (cobrado no encerramento)");
    }

    public int getMinutos() {
        int i = tempo.getIndice();
        return i < 0 ? 30 : MINUTOS[i];
    }
}
