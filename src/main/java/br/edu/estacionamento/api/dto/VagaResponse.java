package br.edu.estacionamento.api.dto;

import br.edu.estacionamento.vaga.Vaga;

/** Vaga com o nome do estado atual: LIVRE, OCUPADA ou RESERVADA. */
public record VagaResponse(Long id, int numero, String estado) {

    public static VagaResponse de(Vaga vaga) {
        return new VagaResponse(vaga.getId(), vaga.getNumero(), vaga.getNomeEstado());
    }
}
