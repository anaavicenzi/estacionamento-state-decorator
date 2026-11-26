package br.edu.estacionamento.servico;

public class VagaCoberta extends ServicoDecorator {

    public static final double VALOR = 15.0;

    public VagaCoberta(Servico servico) {
        super(servico);
    }

    @Override
    public String descricao() {
        return servico.descricao() + " + Vaga Coberta";
    }

    @Override
    public double custo() {
        return servico.custo() + VALOR;
    }
}