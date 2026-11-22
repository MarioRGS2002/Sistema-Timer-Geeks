package geeks.view;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.time.LocalTime;

/** Formulário da janela "Alterar reserva": novo horário. */
public class FormHorario extends FormularioGeeks {
    private final SeletorHorario horario;

    public FormHorario(LocalTime atual) {
        super("Alterar reserva", "Salvar horário", Icone.Tipo.CHECK);
        horario = new SeletorHorario(atual);
        JPanel envolve = Componentes.transparente(new BorderLayout());
        envolve.add(bloco("Novo horário", Icone.Tipo.RELOGIO, horario), BorderLayout.NORTH);
        getCorpo().add(envolve, BorderLayout.CENTER);
        setResumo("Use os botões para ajustar de 30 em 30 minutos.");
    }

    public LocalTime getHora() {
        return horario.getHora();
    }
}
