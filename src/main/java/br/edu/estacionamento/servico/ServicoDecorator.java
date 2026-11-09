package br.edu.estacionamento.servico;

/**
 * Decorator abstrato: implementa Servico e guarda o Servico que está sendo
 * "embrulhado". Por padrão apenas repassa as chamadas; os decorators
 * concretos acrescentam a sua parte na descrição e no custo.
 */
public abstract class ServicoDecorator implements Servico {

    protected final Servico servico;

    protected ServicoDecorator(Servico servico) {
        if (servico == null) {
            throw new IllegalArgumentException("Serviço decorado não pode ser nulo");
        }
        this.servico = servico;
    }

    @Override
    public String descricao() {
        return servico.descricao();
    }

    @Override
    public double custo() {
        return servico.custo();
    }
}
