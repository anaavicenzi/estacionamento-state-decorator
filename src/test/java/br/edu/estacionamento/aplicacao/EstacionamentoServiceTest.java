package br.edu.estacionamento.aplicacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.estacionamento.dominio.Permanencia;
import br.edu.estacionamento.dominio.RegraNegocioException;
import br.edu.estacionamento.vaga.TransicaoInvalidaException;
import br.edu.estacionamento.vaga.Vaga;

class EstacionamentoServiceTest {

    private RelogioAjustavel relogio;
    private EstacionamentoService service;

    @BeforeEach
    void setUp() {
        relogio = new RelogioAjustavel(Instant.parse("2026-11-10T08:00:00Z"));
        service = new EstacionamentoService(new VagaRepository(), new PermanenciaRepository(), relogio);
    }

    @Test
    void deveCriarVagasLivresOrdenadasPorNumero() {
        service.criarVaga(3);
        service.criarVaga(1);

        assertEquals(2, service.listarVagas().size());
        assertEquals(1, service.listarVagas().get(0).getNumero());
        assertEquals("LIVRE", service.listarVagas().get(0).getNomeEstado());
    }

    @Test
    void naoDeveCriarVagaComNumeroRepetidoOuInvalido() {
        service.criarVaga(1);

        assertThrows(RegraNegocioException.class, () -> service.criarVaga(1));
        assertThrows(IllegalArgumentException.class, () -> service.criarVaga(0));
        assertThrows(IllegalArgumentException.class, () -> service.criarVaga(null));
    }

    @Test
    void deveReservarELiberarVaga() {
        Vaga vaga = service.criarVaga(1);

        assertEquals("RESERVADA", service.reservarVaga(vaga.getId()).getNomeEstado());
        assertEquals("LIVRE", service.liberarVaga(vaga.getId()).getNomeEstado());
    }

    @Test
    void deveLancarNaoEncontradoParaVagaInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.reservarVaga(99L));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.registrarEntrada("ABC1D23", "Gol", 99L));
    }

    @Test
    void entradaDeveOcuparVaga() {
        Vaga vaga = service.criarVaga(2);

        Permanencia p = service.registrarEntrada("ABC1D23", "Gol", vaga.getId());

        assertEquals("OCUPADA", vaga.getNomeEstado());
        assertEquals(service.agora(), p.getEntrada());
    }

    @Test
    void entradaDevePermitirVagaReservada() {
        Vaga vaga = service.criarVaga(1);
        service.reservarVaga(vaga.getId());

        service.registrarEntrada("ABC1D23", "Gol", vaga.getId());

        assertEquals("OCUPADA", vaga.getNomeEstado());
    }

    @Test
    void naoDeveOcuparVagaJaOcupada() {
        Vaga vaga = service.criarVaga(2);
        service.registrarEntrada("ABC1D23", "Gol", vaga.getId());

        assertThrows(TransicaoInvalidaException.class,
                () -> service.registrarEntrada("XYZ9K87", "Uno", vaga.getId()));
        assertEquals(1, service.listarPermanencias().size());
    }

    @Test
    void naoDevePermitirMesmoVeiculoEmDuasVagas() {
        Vaga v1 = service.criarVaga(1);
        Vaga v2 = service.criarVaga(2);
        service.registrarEntrada("ABC1D23", "Gol", v1.getId());

        assertThrows(RegraNegocioException.class,
                () -> service.registrarEntrada("abc1d23", "Gol", v2.getId()));
        assertEquals("LIVRE", v2.getNomeEstado());
    }

    @Test
    void naoDeveLiberarManualmenteVagaComPermanenciaAtiva() {
        Vaga vaga = service.criarVaga(1);
        service.registrarEntrada("ABC1D23", "Gol", vaga.getId());

        assertThrows(RegraNegocioException.class, () -> service.liberarVaga(vaga.getId()));
        assertEquals("OCUPADA", vaga.getNomeEstado());
    }

    @Test
    void fluxoCompletoDeveSomarTarifaEExtrasELiberarVaga() {
        Vaga vaga = service.criarVaga(2);
        Permanencia p = service.registrarEntrada("ABC1D23", "Gol", vaga.getId());
        service.adicionarServico(p.getId(), "LAVAGEM");
        service.adicionarServico(p.getId(), "manobrista");
        relogio.avancar(Duration.ofHours(2).plusMinutes(30));

        service.registrarSaida(p.getId());

        assertEquals("Tarifa 2h + Lavagem + Manobrista", p.getServico().descricao());
        assertEquals(70.0, p.getValorTotal());
        assertEquals("LIVRE", vaga.getNomeEstado());
    }

    @Test
    void veiculoPodeVoltarAposSaida() {
        Vaga vaga = service.criarVaga(1);
        Permanencia p = service.registrarEntrada("ABC1D23", "Gol", vaga.getId());
        service.registrarSaida(p.getId());

        service.registrarEntrada("ABC1D23", "Gol", vaga.getId());

        assertEquals(2, service.listarPermanencias().size());
        assertEquals("OCUPADA", vaga.getNomeEstado());
    }

    @Test
    void naoDeveAdicionarServicoDuplicadoOuInvalido() {
        Vaga vaga = service.criarVaga(1);
        Permanencia p = service.registrarEntrada("ABC1D23", "Gol", vaga.getId());
        service.adicionarServico(p.getId(), "LAVAGEM");

        assertThrows(RegraNegocioException.class, () -> service.adicionarServico(p.getId(), "LAVAGEM"));
        assertThrows(IllegalArgumentException.class, () -> service.adicionarServico(p.getId(), "XPTO"));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.adicionarServico(99L, "LAVAGEM"));
    }

    @Test
    void naoDeveRegistrarSaidaDuasVezes() {
        Vaga vaga = service.criarVaga(1);
        Permanencia p = service.registrarEntrada("ABC1D23", "Gol", vaga.getId());
        service.registrarSaida(p.getId());

        assertThrows(RegraNegocioException.class, () -> service.registrarSaida(p.getId()));
    }
}
