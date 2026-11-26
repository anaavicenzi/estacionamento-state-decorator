package br.edu.estacionamento.servico;

public class RecargaEletrica extends ServicoDecorator {

    public static final double VALOR = 25.0;

    public RecargaEletrica(Servico servico) {
        super(servico);
    }

    @Override
    public String descricao() {
        return servico.descricao() + " + Recarga Elétrica";
    }

    @Override
    public double custo() {
        return servico.custo() + VALOR;
    }
}