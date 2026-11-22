package geeks.view;

import geeks.model.Cliente;
import geeks.model.Dispositivo;
import geeks.model.GeeksModel;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Formulário da janela "Registrar reserva": cliente, dispositivo, horário, duração e, se o cliente
 * já pagou, a entrada (com a forma de pagamento).
 */
public class FormReserva extends FormularioGeeks {
    private static final int[] MINUTOS = {30, 60, 90, 120, 180, 240};
    private static final List<String> ROTULOS = List.of("30 min", "1 h", "1h30", "2 h", "3 h", "4 h");

    private final SeletorCliente cliente;
    private final SeletorDispositivo dispositivo;
    private final SeletorHorario horario = new SeletorHorario(LocalTime.of(14, 0));
    private final SeletorOpcoes duracao = new SeletorOpcoes(ROTULOS, 1);
    private final CaixaMarcacao chkEntrada = new CaixaMarcacao("Cliente pagou a entrada agora", true);
    private final SeletorOpcoes forma;

    public FormReserva(List<Cliente> clientes, List<Dispositivo> dispositivos, LocalDate dia, List<String> formas) {
        super("Registrar reserva", "Registrar reserva", Icone.Tipo.CHECK);
        cliente = new SeletorCliente(clientes, 2);
        dispositivo = new SeletorDispositivo(dispositivos, d -> false);
        forma = new SeletorOpcoes(formas, 0);

        JPanel quando = Componentes.transparente(new BorderLayout(0, 12));
        quando.add(horario, BorderLayout.NORTH);
        quando.add(duracao, BorderLayout.CENTER);

        JPanel entrada = Componentes.transparente(new BorderLayout(0, 8));
        JPanel linhaChk = Componentes.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaChk.add(chkEntrada);
        entrada.add(linhaChk, BorderLayout.NORTH);
        entrada.add(forma, BorderLayout.CENTER);

        JPanel parteBaixa = Componentes.transparente(new BorderLayout(0, 16));
        parteBaixa.add(bloco("Dispositivo", Icone.Tipo.CONTROLE, dispositivo), BorderLayout.NORTH);
        parteBaixa.add(bloco("Horário e duração em " + Formatador.dataExtenso(dia), Icone.Tipo.CALENDARIO, quando), BorderLayout.CENTER);
        parteBaixa.add(bloco("Entrada", Icone.Tipo.MOEDA, entrada), BorderLayout.SOUTH);
        JPanel empilhado = Componentes.transparente(new BorderLayout(0, 16));
        empilhado.add(bloco("Cliente", Icone.Tipo.USUARIO_MAIS, cliente), BorderLayout.NORTH);
        empilhado.add(parteBaixa, BorderLayout.CENTER);
        getCorpo().add(empilhado, BorderLayout.CENTER);

        cliente.addListenerSelecao(this::atualizar);
        dispositivo.addListenerSelecao(this::atualizar);
        duracao.addListenerSelecao(this::atualizar);
        chkEntrada.addActionListener(e -> atualizar());
        atualizar();
    }

    private void atualizar() {
        boolean ok = cliente.getSelecionado() != null && dispositivo.getSelecionado() != null;
        setConfirmarHabilitado(ok);
        forma.setEnabled(chkEntrada.isSelected());
        if (!ok) {
            setResumo("Escolha o cliente e o dispositivo da reserva.");
            return;
        }
        Dispositivo d = dispositivo.getSelecionado();
        setResumo("Valor previsto " + Formatador.moeda(GeeksModel.valorReserva(d, getMinutos()))
                + "  |  entrada " + Formatador.moeda(GeeksModel.entradaReserva(d, getMinutos()))
                + (chkEntrada.isSelected() ? " paga agora" : " (pode pagar depois)"));
    }

    public Cliente getCliente() {
        return cliente.getSelecionado();
    }

    public Dispositivo getDispositivo() {
        return dispositivo.getSelecionado();
    }

    public LocalTime getHora() {
        return horario.getHora();
    }

    public int getMinutos() {
        int i = duracao.getIndice();
        return i < 0 ? 60 : MINUTOS[i];
    }

    /** Forma em que a entrada foi paga agora, ou null se não pagou. */
    public String getFormaEntrada() {
        return chkEntrada.isSelected() ? forma.getSelecionada() : null;
    }
}
