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

    @Test
    void vagaLivreDeveIniciarManutencao() {
        assertInstanceOf(VagaManutencao.class, new VagaLivre().iniciarManutencao());
    }

    @Test
    void vagaOcupadaNaoPodeIniciarManutencao() {
        assertThrows(TransicaoInvalidaException.class, () -> new VagaOcupada().iniciarManutencao());
    }

    @Test
    void vagaReservadaNaoPodeIniciarManutencao() {
        assertThrows(TransicaoInvalidaException.class, () -> new VagaReservada().iniciarManutencao());
    }

    @Test
    void vagaManutencaoNaoPodeSerOcupada() {
        assertThrows(TransicaoInvalidaException.class, () -> new VagaManutencao().ocupar());
    }

    @Test
    void vagaManutencaoNaoPodeSerReservada() {
        assertThrows(TransicaoInvalidaException.class, () -> new VagaManutencao().reservar());
    }

    @Test
    void vagaManutencaoNaoPodeIniciarManutencaoDeNovo() {
        assertThrows(TransicaoInvalidaException.class, () -> new VagaManutencao().iniciarManutencao());
    }

    @Test
    void vagaManutencaoDeveLiberar() {
        assertInstanceOf(VagaLivre.class, new VagaManutencao().liberar());
    }
}
