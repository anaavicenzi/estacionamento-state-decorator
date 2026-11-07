package br.edu.estacionamento.vaga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VagaTest {

    @Test
    void deveIniciarLivre() {
        Vaga vaga = new Vaga(1L, 10);

        assertEquals(1L, vaga.getId());
        assertEquals(10, vaga.getNumero());
        assertInstanceOf(VagaLivre.class, vaga.getEstado());
        assertEquals("LIVRE", vaga.getNomeEstado());
    }

    @Test
    void deveSeguirFluxoLivreReservadaOcupadaLivre() {
        Vaga vaga = new Vaga(1L, 10);

        vaga.reservar();
        assertEquals("RESERVADA", vaga.getNomeEstado());
        vaga.ocupar();
        assertEquals("OCUPADA", vaga.getNomeEstado());
        vaga.liberar();

        assertEquals("LIVRE", vaga.getNomeEstado());
    }

    @Test
    void naoDeveMudarEstadoQuandoOcuparVagaOcupada() {
        Vaga vaga = new Vaga(1L, 10);
        vaga.ocupar();

        assertThrows(TransicaoInvalidaException.class, vaga::ocupar);

        assertEquals("OCUPADA", vaga.getNomeEstado());
    }

    @Test
    void nomeDoEstadoDeveRefletirEstadoAtual() {
        Vaga vaga = new Vaga(1L, 10);

        assertEquals("LIVRE", vaga.getNomeEstado());
        vaga.reservar();
        assertEquals("RESERVADA", vaga.getNomeEstado());
    }
}
