package geeks.view;

import geeks.model.Cliente;
import geeks.model.Dispositivo;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Formulário da janela "Registrar reserva": cliente, dispositivo e horário. */
public class FormReserva extends FormularioGeeks {
    private final SeletorCliente cliente;
    private final SeletorDispositivo dispositivo;
    private final SeletorHorario horario = new SeletorHorario(LocalTime.of(14, 0));

    public FormReserva(List<Cliente> clientes, List<Dispositivo> dispositivos, LocalDate dia) {
        super("Registrar reserva", "Registrar reserva", Icone.Tipo.CHECK);
        cliente = new SeletorCliente(clientes, 2);
        dispositivo = new SeletorDispositivo(dispositivos, d -> false);

        JPanel parteBaixa = Componentes.transparente(new BorderLayout(0, 16));
        parteBaixa.add(bloco("Dispositivo", Icone.Tipo.CONTROLE, dispositivo), BorderLayout.NORTH);
        parteBaixa.add(bloco("Horário em " + Formatador.dataExtenso(dia), Icone.Tipo.CALENDARIO, horario), BorderLayout.CENTER);
        JPanel empilhado = Componentes.transparente(new BorderLayout(0, 16));
        empilhado.add(bloco("Cliente", Icone.Tipo.USUARIO_MAIS, cliente), BorderLayout.NORTH);
        empilhado.add(parteBaixa, BorderLayout.CENTER);
        getCorpo().add(empilhado, BorderLayout.CENTER);

        cliente.addListenerSelecao(this::atualizar);
        dispositivo.addListenerSelecao(this::atualizar);
        atualizar();
    }

    private void atualizar() {
        setConfirmarHabilitado(cliente.getSelecionado() != null && dispositivo.getSelecionado() != null);
        setResumo(cliente.getSelecionado() == null || dispositivo.getSelecionado() == null
                ? "Escolha o cliente e o dispositivo da reserva." : " ");
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
}
