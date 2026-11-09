package br.edu.estacionamento.servico;

/**
 * Decorator concreto: acrescenta o serviço de lavagem (valor fixo).
 */
public class Lavagem extends ServicoDecorator {

    public static final double VALOR = 30.0;

    public Lavagem(Servico servico) {
        super(servico);
    }

    @Override
    public String descricao() {
        return servico.descricao() + " + Lavagem";
    }

    @Override
    public double custo() {
        return servico.custo() + VALOR;
    }
}
