package geeks.view;

import geeks.model.Cliente;
import geeks.model.Dispositivo;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;

/** Formulário da janela "Nova sessão": cliente, dispositivo livre e o tempo (livre ou fechado). */
public class FormNovaSessao extends FormularioGeeks {
    /** Opções de tempo: null = livre (sem hora para acabar). */
    private static final Integer[] MINUTOS = {null, 30, 60, 90, 120, 180, 240};
    private static final List<String> ROTULOS = List.of("Livre", "30 min", "1 h", "1h30", "2 h", "3 h", "4 h");

    private final SeletorCliente cliente;
    private final SeletorDispositivo dispositivo;
    private final SeletorOpcoes tempo = new SeletorOpcoes(ROTULOS, 0);

    public FormNovaSessao(List<Cliente> clientes, List<Dispositivo> dispositivos, Predicate<Dispositivo> ocupado) {
        super("Nova sessão", "Iniciar sessão", Icone.Tipo.RAIO);
        cliente = new SeletorCliente(clientes, 3);
        dispositivo = new SeletorDispositivo(dispositivos, ocupado);

        JPanel corpo = getCorpo();
        JPanel dispositivos2 = Componentes.transparente(new BorderLayout(0, 16));
        dispositivos2.add(bloco("Dispositivo", Icone.Tipo.CONTROLE, dispositivo), BorderLayout.NORTH);
        dispositivos2.add(bloco("Tempo", Icone.Tipo.RELOGIO, tempo), BorderLayout.CENTER);
        JPanel empilhado = Componentes.transparente(new BorderLayout(0, 16));
        empilhado.add(bloco("Cliente", Icone.Tipo.USUARIO_MAIS, cliente), BorderLayout.NORTH);
        empilhado.add(dispositivos2, BorderLayout.CENTER);
        corpo.add(empilhado, BorderLayout.CENTER);

        cliente.addListenerSelecao(this::atualizar);
        dispositivo.addListenerSelecao(this::atualizar);
        tempo.addListenerSelecao(this::atualizar);
        atualizar();
    }

    private void atualizar() {
        Cliente c = cliente.getSelecionado();
        Dispositivo d = dispositivo.getSelecionado();
        setConfirmarHabilitado(c != null && d != null);
        if (c == null || d == null) {
            setResumo("Escolha o cliente, o dispositivo e o tempo para iniciar.");
            return;
        }
        Integer min = getMinutos();
        String quanto;
        if (min == null) {
            quanto = "tempo livre (mínimo de 1 h) - " + Formatador.moeda(d.getTipo().getValorHora()) + " por hora";
        } else {
            BigDecimal valor = d.getTipo().getValorHora().multiply(BigDecimal.valueOf(min))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            quanto = "até " + LocalTime.now().plusMinutes(min).format(DateTimeFormatter.ofPattern("HH:mm"))
                    + " - " + Formatador.moeda(valor);
        }
        setResumo(c.getNome() + " em " + d.getNome() + ", " + quanto);
    }

    public Cliente getCliente() {
        return cliente.getSelecionado();
    }

    public Dispositivo getDispositivo() {
        return dispositivo.getSelecionado();
    }

    /** Minutos de tempo fechado, ou null para tempo livre. */
    public Integer getMinutos() {
        int i = tempo.getIndice();
        return i < 0 ? null : MINUTOS[i];
    }
}
