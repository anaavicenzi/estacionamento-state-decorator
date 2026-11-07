package br.edu.estacionamento.vaga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EstadoVagaTest {

    @Test
    void vagaLivreDeveOcupar() {
        assertInstanceOf(VagaOcupada.class, new VagaLivre().ocupar());
    }

    @Test
    void vagaLivreNaoPodeSerLiberada() {
        TransicaoInvalidaException exception = assertThrows(
                TransicaoInvalidaException.class, () -> new VagaLivre().liberar());

        assertEquals("Não é possível liberar uma vaga no estado LIVRE", exception.getMessage());
    }

    @Test
    void vagaLivreDeveReservar() {
        assertInstanceOf(VagaReservada.class, new VagaLivre().reservar());
    }

    @Test
    void vagaOcupadaNaoPodeSerOcupada() {
        assertThrows(TransicaoInvalidaException.class, () -> new VagaOcupada().ocupar());
    }

    @Test
    void vagaOcupadaDeveLiberar() {
        assertInstanceOf(VagaLivre.class, new VagaOcupada().liberar());
    }

    @Test
    void vagaOcupadaNaoPodeSerReservada() {
        assertThrows(TransicaoInvalidaException.class, () -> new VagaOcupada().reservar());
    }

    @Test
    void vagaReservadaDeveOcupar() {
        assertInstanceOf(VagaOcupada.class, new VagaReservada().ocupar());
    }

    @Test
    void vagaReservadaDeveLiberar() {
        assertInstanceOf(VagaLivre.class, new VagaReservada().liberar());
    }

    @Test
    void vagaReservadaNaoPodeSerReservada() {
        assertThrows(TransicaoInvalidaException.class, () -> new VagaReservada().reservar());
    }
}
