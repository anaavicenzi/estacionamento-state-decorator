package br.edu.estacionamento.servico;

/**
 * Componente concreto do Decorator: a cobrança básica da permanência,
 * calculada como valor por hora × quantidade de horas.
 */
public class TarifaBase implements Servico {

    public static final double VALOR_HORA = 10.0;

    private final int horas;

    public TarifaBase(int horas) {
        if (horas < 0) {
            throw new IllegalArgumentException("Quantidade de horas não pode ser negativa");
        }
        this.horas = horas;
    }

    @Override
    public String descricao() {
        return "Tarifa " + horas + "h";
    }

    @Override
    public double custo() {
        return VALOR_HORA * horas;
    }

    public int getHoras() {
        return horas;
    }
}
