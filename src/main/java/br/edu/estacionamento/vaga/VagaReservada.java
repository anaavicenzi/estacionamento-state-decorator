package br.edu.estacionamento.vaga;

public class VagaReservada implements EstadoVaga {

    @Override
    public EstadoVaga ocupar() {
        return new VagaOcupada();
    }

    @Override
    public EstadoVaga liberar() {
        return new VagaLivre();
    }

    @Override
    public EstadoVaga reservar() {
        throw new TransicaoInvalidaException("reservar", nome());
    }

    @Override
    public String nome() {
        return "RESERVADA";
    }
}
