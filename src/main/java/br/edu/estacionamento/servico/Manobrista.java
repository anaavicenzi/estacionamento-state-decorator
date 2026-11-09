package br.edu.estacionamento.servico;

/**
 * Decorator concreto: acrescenta o serviço de manobrista (valor fixo).
 */
public class Manobrista extends ServicoDecorator {

    public static final double VALOR = 20.0;

    public Manobrista(Servico servico) {
        super(servico);
    }

    @Override
    public String descricao() {
        return servico.descricao() + " + Manobrista";
    }

    @Override
    public double custo() {
        return servico.custo() + VALOR;
    }
}
