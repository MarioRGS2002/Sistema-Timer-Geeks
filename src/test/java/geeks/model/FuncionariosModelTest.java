package geeks.model;

import geeks.BancoTeste;
import geeks.dados.Sql;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static geeks.BancoTeste.app;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gestão de funcionários pelo gerente: cadastro, senha, desativação e as regras de acesso. */
@Tag("db")
class FuncionariosModelTest {
    private final FuncionariosModel model = new FuncionariosModel();
    private final LoginModel login = new LoginModel();
    private int idChefe;

    @BeforeEach
    void preparar() throws SQLException {
        BancoTeste.resetar();
        idChefe = BancoTeste.logar("chefe", "GERENTE").getIdFuncionario();
    }

    @AfterEach
    void sair() {
        Contexto.sair();
    }

    private static char[] s(String senha) {
        return senha.toCharArray();
    }

    private Funcionario cadastrar(String l, Perfil p, String senha) {
        return model.cadastrar(l, "Nome " + l, p, s(senha), s(senha));
    }

    private void voltarComoChefe() {
        assertNotNull(login.autenticar("chefe", s("senha-teste-123")));
    }

    @Test
    void gerenteCadastraEOFuncionarioEntra() {
        Funcionario f = model.cadastrar("  Maria.S ", "Maria Silva", Perfil.ATENDENTE, s("senha-maria"), s("senha-maria"));
        assertEquals("maria.s", f.getLogin(), "login normalizado");
        assertEquals(2, model.listar().size());
        Usuario u = login.autenticar("maria.s", s("senha-maria"));
        assertNotNull(u);
        assertFalse(u.isGerente());
    }

    @Test
    void cadastroRecusaDadosInvalidos() {
        cadastrar("maria", Perfil.ATENDENTE, "senha-maria");
        assertThrows(IllegalArgumentException.class, () -> cadastrar("maria", Perfil.ATENDENTE, "outra-senha"), "login repetido");
        assertThrows(IllegalArgumentException.class, () -> cadastrar("CHEFE", Perfil.ATENDENTE, "outra-senha"), "repetido ignorando maiúsculas");
        assertThrows(IllegalArgumentException.class, () -> cadastrar("jo", Perfil.ATENDENTE, "senha-joao"), "login curto");
        assertThrows(IllegalArgumentException.class, () -> cadastrar("joão", Perfil.ATENDENTE, "senha-joao"), "acento no login");
        assertThrows(IllegalArgumentException.class, () -> cadastrar("joao", Perfil.ATENDENTE, "curta"), "senha curta");
        assertThrows(IllegalArgumentException.class,
                () -> model.cadastrar("joao", "João", Perfil.ATENDENTE, s("senha-joao"), s("senha-diferente")), "confirmação");
        assertThrows(IllegalArgumentException.class, () -> model.cadastrar("joao", "  ", Perfil.ATENDENTE, s("senha-joao"), s("senha-joao")));
        assertEquals(2, model.listar().size(), "nada inválido foi gravado");
    }

    @Test
    void senhaFicaSoComoHashEOVetorEApagado() throws SQLException {
        char[] senha = s("senha-secreta");
        char[] conf = s("senha-secreta");
        Funcionario f = model.cadastrar("ana", "Ana", Perfil.ATENDENTE, senha, conf);
        assertArrayEquals(new char[13], senha, "a senha foi apagada da memória");
        try (Connection c = app()) {
            assertEquals(1, Sql.contar(c, "SELECT COUNT(*) FROM funcionario WHERE id_funcionario = ? "
                    + "AND senha_hash LIKE '$2a$11$%' AND senha_hash <> 'senha-secreta'", f.getId()));
        }
    }

    @Test
    void atendenteNaoGerenciaFuncionarios() {
        cadastrar("maria", Perfil.ATENDENTE, "senha-maria");
        assertNotNull(login.autenticar("maria", s("senha-maria")));
        assertThrows(SecurityException.class, model::listar);
        assertThrows(SecurityException.class, () -> cadastrar("intruso", Perfil.GERENTE, "senha-intruso"));
        assertThrows(SecurityException.class, () -> model.trocarSenha(idChefe, s("roubada-123"), s("roubada-123")));
        assertThrows(SecurityException.class, () -> model.desativar(idChefe));
        Contexto.sair();
        assertThrows(SecurityException.class, model::listar, "sem login");
    }

    @Test
    void desativadoNaoEntraEReativadoVolta() {
        Funcionario f = cadastrar("maria", Perfil.ATENDENTE, "senha-maria");
        model.desativar(f.getId());
        assertNull(login.autenticar("maria", s("senha-maria")));
        voltarComoChefe();
        assertFalse(model.listar().stream().filter(x -> x.getId() == f.getId()).findFirst().orElseThrow().isAtivo(),
                "continua na lista, desativado (nada é apagado)");
        model.reativar(f.getId());
        assertNotNull(login.autenticar("maria", s("senha-maria")));
    }

    @Test
    void naoDesativaASiMesmoNemOUltimoGerente() {
        assertThrows(IllegalArgumentException.class, () -> model.desativar(idChefe), "a si mesmo");
        Funcionario outro = cadastrar("dona", Perfil.GERENTE, "senha-dona-1");
        model.desativar(outro.getId());
        assertNotNull(login.autenticar("chefe", s("senha-teste-123")));
        model.reativar(outro.getId());
        // agora o "dona" desativa o "chefe" (sobra 1 gerente ativo: ela mesma)
        assertNotNull(login.autenticar("dona", s("senha-dona-1")));
        assertDoesNotThrow(() -> model.desativar(idChefe));
        assertNull(login.autenticar("chefe", s("senha-teste-123")));
    }

    @Test
    void gerenteDesativadoEmOutroComputadorPerdeOAcessoNaHora() throws SQLException {
        Funcionario dona = cadastrar("dona", Perfil.GERENTE, "senha-dona-1");
        // o chefe continua com o sistema aberto, mas foi desativado (por outro gerente, em outro computador)
        try (Connection c = app()) {
            Sql.atualizar(c, "UPDATE funcionario SET ativo = 0 WHERE id_funcionario = ?", idChefe);
        }
        assertThrows(SecurityException.class, () -> model.desativar(dona.getId()),
                "gerente desativado em outro computador perde o acesso na hora");
        try (Connection c = app()) {
            assertEquals(1, Sql.contar(c, "SELECT ativo FROM funcionario WHERE id_funcionario = ?", dona.getId()));
        }
    }

    @Test
    void trocarSenhaInvalidaAAntiga() {
        Funcionario f = cadastrar("maria", Perfil.ATENDENTE, "senha-antiga");
        model.trocarSenha(f.getId(), s("senha-nova-1"), s("senha-nova-1"));
        assertThrows(IllegalArgumentException.class, () -> model.trocarSenha(f.getId(), s("curta"), s("curta")));
        assertNull(login.autenticar("maria", s("senha-antiga")));
        assertNotNull(login.autenticar("maria", s("senha-nova-1")));
    }

    @Test
    void doisGerentesDesativandoUmAoOutroAoMesmoTempoSobraUm() throws Exception {
        Funcionario dona = cadastrar("dona", Perfil.GERENTE, "senha-dona-1");
        // Simula os dois computadores: cada thread age como um gerente diferente.
        // Contexto é estático, então as duas ações rodam no mesmo login trocando o alvo:
        // o chefe tenta desativar a dona enquanto a dona (via banco) tenta desativar o chefe.
        java.util.concurrent.ExecutorService ex = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch largada = new java.util.concurrent.CountDownLatch(1);
        try {
            var a = ex.submit(() -> {
                largada.await();
                try {
                    model.desativar(dona.getId());
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            });
            var b = ex.submit(() -> {
                largada.await();
                // mesma regra direto no banco, como o outro computador faria
                try {
                    return geeks.dados.Transacao.executar(c -> {
                        int gerentes = new geeks.dados.FuncionarioDAO().gerentesAtivosTravando(c);
                        if (gerentes <= 1) {
                            return false;
                        }
                        new geeks.dados.FuncionarioDAO().definirAtivo(c, idChefe, false);
                        return true;
                    });
                } catch (RuntimeException e) {
                    return false;
                }
            });
            largada.countDown();
            a.get(30, java.util.concurrent.TimeUnit.SECONDS);
            b.get(30, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            ex.shutdownNow();
        }
        try (Connection c = app()) {
            assertTrue(Sql.contar(c, "SELECT COUNT(*) FROM funcionario WHERE cargo = 'GERENTE' AND ativo = 1") >= 1,
                    "nunca fica sem gerente ativo");
        }
    }
}
