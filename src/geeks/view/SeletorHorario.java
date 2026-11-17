package geeks.view;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Escolha de horário em passos de 30 minutos: "− 30 min   14:00   + 30 min". */
public class SeletorHorario extends CartaoGeeks {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    private LocalTime hora;
    private final JLabel lbHora = Componentes.rotulo("", Font.BOLD | Font.ITALIC, 38, Tema.AMARELO);

    public SeletorHorario(LocalTime inicial) {
        super(Tema.CAMPO, new java.awt.Color(0x333333), false);
        this.hora = inicial;
        setLayout(new FlowLayout(FlowLayout.CENTER, 22, 0));
        setBorder(new EmptyBorder(8, 14, 8, 14));
        lbHora.setIcon(new Icone(Icone.Tipo.RELOGIO, 36, Tema.AMARELO));
        lbHora.setIconTextGap(12);
        BotaoGeeks menos = new BotaoGeeks("\u2212 30 min", null, BotaoGeeks.Estilo.CONTORNO);
        BotaoGeeks mais = new BotaoGeeks("+ 30 min", null, BotaoGeeks.Estilo.CONTORNO);
        menos.addActionListener(e -> mudar(-30));
        mais.addActionListener(e -> mudar(30));
        add(menos);
        add(lbHora);
        add(mais);
        atualizar();
    }

    private void mudar(int minutos) {
        hora = hora.plusMinutes(minutos);
        atualizar();
    }

    private void atualizar() {
        lbHora.setText(hora.format(FORMATO));
    }

    public LocalTime getHora() {
        return hora;
    }
}
