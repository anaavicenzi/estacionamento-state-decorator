package br.edu.estacionamento.vaga;

public class VagaLivre implements EstadoVaga {

    @Override
    public EstadoVaga ocupar() {
        return new VagaOcupada();
    }

    @Override
    public EstadoVaga liberar() {
        throw new TransicaoInvalidaException("liberar", nome());
    }

    @Override
    public EstadoVaga reservar() {
        return new VagaReservada();
    }

    @Override
    public String nome() {
        return "LIVRE";
    }
}
