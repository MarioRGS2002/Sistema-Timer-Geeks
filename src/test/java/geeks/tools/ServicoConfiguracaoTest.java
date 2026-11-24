package geeks.tools;

import geeks.BancoTeste;
import geeks.model.LoginModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Passos do assistente de configuração que mexem no banco (gerente e equipamentos). */
@Tag("db")
class ServicoConfiguracaoTest {
    private final ServicoConfiguracao servico = new ServicoConfiguracao();

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
    }

    @Test
    void cadastraOGerenteQueEntraNoSistema() {
        assertNull(servico.gerenteExistente());
        assertThrows(IllegalArgumentException.class,
                () -> servico.cadastrarGerente("dona", "Dona da Loja", "curta".toCharArray(), "curta".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> servico.cadastrarGerente("dona", "Dona da Loja", "senha-boa-1".toCharArray(), "outra-senha".toCharArray()));
        char[] senha = "senha-boa-1".toCharArray();
        servico.cadastrarGerente(" Dona ", "Dona da Loja", senha, "senha-boa-1".toCharArray());
        assertArrayEquals(new char[11], senha, "a senha é apagada da memória");
        assertEquals("Dona da Loja", servico.gerenteExistente());
        assertNotNull(new LoginModel().autenticar("dona", "senha-boa-1".toCharArray()));
    }

    @Test
    void cadastraEquipamentosSemDuplicar() {
        assertEquals(List.of("PC", "PlayStation", "Nintendo Switch", "Xadrez"), servico.tiposDeEquipamento());
        int n = servico.cadastrarEquipamentos(List.of(new String[]{"PC01", "PC"}, new String[]{"Ps5", "PlayStation"}));
        assertEquals(2, n);
        assertEquals(0, servico.cadastrarEquipamentos(List.<String[]>of(new String[]{"PC01", "PC"})), "já existe: ignora");
        assertEquals(List.of("PC01 (PC)", "Ps5 (PlayStation)"), servico.equipamentos());
        assertThrows(IllegalArgumentException.class, () -> ServicoConfiguracao.validarEquipamento("  "));
        assertThrows(IllegalArgumentException.class, () -> ServicoConfiguracao.validarEquipamento("x".repeat(51)));
    }
}
