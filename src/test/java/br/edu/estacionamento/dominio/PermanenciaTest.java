package br.edu.estacionamento.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.estacionamento.servico.Servico;
import br.edu.estacionamento.vaga.Vaga;

class PermanenciaTest {

    private static final LocalDateTime ENTRADA = LocalDateTime.of(2026, 11, 10, 8, 0);

    private Vaga vaga;
    private Permanencia permanencia;

    @BeforeEach
    void setUp() {
        vaga = new Vaga(1L, 1);
        vaga.ocupar();
        permanencia = new Permanencia(1L, vaga, new Veiculo("abc1d23", "Gol"), ENTRADA);
    }

    @Test
    void deveIniciarAtivaSemValorTotal() {
        assertFalse(permanencia.isFinalizada());
        assertNull(permanencia.getSaida());
        assertNull(permanencia.getValorTotal());
        assertNull(permanencia.getServico());
    }

    @Test
    void deveCalcularSoTarifaQuandoNaoHaExtras() {
        permanencia.finalizar(ENTRADA.plusHours(3));

        assertEquals("Tarifa 3h", permanencia.getServico().descricao());
        assertEquals(30.0, permanencia.getValorTotal());
    }

    @Test
    void deveMontarCadeiaDeDecoratorsNaOrdemDosPedidos() {
        permanencia.adicionarServico(TipoServico.LAVAGEM);
        permanencia.adicionarServico(TipoServico.MANOBRISTA);

        permanencia.finalizar(ENTRADA.plusHours(2));

        assertEquals("Tarifa 2h + Lavagem + Manobrista", permanencia.getServico().descricao());
        assertEquals(20.0 + 30.0 + 20.0, permanencia.getValorTotal());
        assertEquals(List.of(TipoServico.LAVAGEM, TipoServico.MANOBRISTA), permanencia.getExtras());
    }

    @Test
    void deveArredondarFracaoDeHoraParaCima() {
        permanencia.finalizar(ENTRADA.plusMinutes(61));

        assertEquals("Tarifa 2h", permanencia.getServico().descricao());
        assertEquals(20.0, permanencia.getValorTotal());
    }

    @Test
    void naoDeveArredondarHoraExata() {
        permanencia.finalizar(ENTRADA.plusMinutes(60));

        assertEquals("Tarifa 1h", permanencia.getServico().descricao());
    }

    @Test
    void deveCobrarNoMinimoUmaHora() {
        assertEquals("Tarifa 1h", permanencia.servicoAte(ENTRADA).descricao());
        assertEquals("Tarifa 1h", permanencia.servicoAte(ENTRADA.plusSeconds(1)).descricao());

        permanencia.finalizar(ENTRADA.plusMinutes(58));

        assertEquals("Tarifa 1h", permanencia.getServico().descricao());
        assertEquals(10.0, permanencia.getValorTotal());
    }

    @Test
    void deveLiberarVagaAoFinalizar() {
        permanencia.finalizar(ENTRADA.plusHours(1));

        assertTrue(permanencia.isFinalizada());
        assertEquals(ENTRADA.plusHours(1), permanencia.getSaida());
        assertEquals("LIVRE", vaga.getNomeEstado());
    }

    @Test
    void deveMostrarCobrancaParcialEnquantoAtiva() {
        permanencia.adicionarServico(TipoServico.LAVAGEM);

        Servico parcial = permanencia.servicoAte(ENTRADA.plusHours(4));

        assertEquals("Tarifa 4h + Lavagem", parcial.descricao());
        assertEquals(70.0, parcial.custo());
        assertFalse(permanencia.isFinalizada());
    }

    @Test
    void cobrancaDeveFicarFixaAposSaida() {
        permanencia.finalizar(ENTRADA.plusHours(1));

        assertEquals("Tarifa 1h", permanencia.servicoAte(ENTRADA.plusHours(10)).descricao());
    }

    @Test
    void naoDevePermitirServicoDuplicado() {
        permanencia.adicionarServico(TipoServico.LAVAGEM);

        assertThrows(RegraNegocioException.class,
                () -> permanencia.adicionarServico(TipoServico.LAVAGEM));
        assertEquals(List.of(TipoServico.LAVAGEM), permanencia.getExtras());
    }

    @Test
    void naoDevePermitirServicoAposSaida() {
        permanencia.finalizar(ENTRADA.plusHours(1));

        assertThrows(RegraNegocioException.class,
                () -> permanencia.adicionarServico(TipoServico.MANOBRISTA));
    }

    @Test
    void naoDevePermitirSaidaDuplicada() {
        permanencia.finalizar(ENTRADA.plusHours(1));

        assertThrows(RegraNegocioException.class,
                () -> permanencia.finalizar(ENTRADA.plusHours(2)));
        assertEquals(ENTRADA.plusHours(1), permanencia.getSaida());
    }

    @Test
    void veiculoDeveNormalizarPlaca() {
        assertEquals("ABC1D23", permanencia.getVeiculo().placa());
    }

    @Test
    void veiculoDeveExigirPlacaEModelo() {
        assertThrows(IllegalArgumentException.class, () -> new Veiculo(" ", "Gol"));
        assertThrows(IllegalArgumentException.class, () -> new Veiculo("ABC1D23", null));
    }

    @Test
    void tipoServicoDeveConverterTextoSemDiferenciarMaiusculas() {
        assertEquals(TipoServico.LAVAGEM, TipoServico.de("lavagem"));
        assertEquals(TipoServico.MANOBRISTA, TipoServico.de(" MANOBRISTA "));
        assertThrows(IllegalArgumentException.class, () -> TipoServico.de("ENCERAMENTO"));
        assertThrows(IllegalArgumentException.class, () -> TipoServico.de(null));
    }
}
