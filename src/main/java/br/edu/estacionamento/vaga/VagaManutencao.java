package br.edu.estacionamento.vaga;

public class VagaManutencao implements EstadoVaga {

    @Override
    public EstadoVaga ocupar() {
        throw new TransicaoInvalidaException("ocupar", nome());
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
    public EstadoVaga iniciarManutencao() {
        throw new TransicaoInvalidaException("colocar em manutenção", nome());
    }

    @Override
    public String nome() {
        return "MANUTENCAO";
    }
}