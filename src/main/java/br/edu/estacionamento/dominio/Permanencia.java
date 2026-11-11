package br.edu.estacionamento.dominio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.edu.estacionamento.servico.Servico;
import br.edu.estacionamento.servico.TarifaBase;
import br.edu.estacionamento.vaga.Vaga;

/**
 * Período em que um veículo ocupa uma vaga.
 *
 * <p>Os extras são guardados na ordem em que foram pedidos. A cadeia de
 * decorators é montada a partir da {@link TarifaBase}, que depende do tempo de
 * permanência; por isso ela só fica fixa na saída. Enquanto a permanência está
 * ativa, {@link #servicoAte(LocalDateTime)} mostra a cobrança parcial.
 */
public class Permanencia {

    private final Long id;
    private final Vaga vaga;
    private final Veiculo veiculo;
    private final LocalDateTime entrada;
    private final List<TipoServico> extras = new ArrayList<>();

    private LocalDateTime saida;
    private Servico servico;
    private Double valorTotal;

    public Permanencia(Long id, Vaga vaga, Veiculo veiculo, LocalDateTime entrada) {
        this.id = id;
        this.vaga = vaga;
        this.veiculo = veiculo;
        this.entrada = entrada;
    }

    public void adicionarServico(TipoServico tipo) {
        if (isFinalizada()) {
            throw new RegraNegocioException("Permanência " + id + " já foi finalizada");
        }
        if (extras.contains(tipo)) {
            throw new RegraNegocioException("Serviço " + tipo + " já foi adicionado à permanência " + id);
        }
        extras.add(tipo);
    }

    /** Registra a saída: fecha a cadeia de serviços, calcula o total e libera a vaga. */
    public void finalizar(LocalDateTime momentoSaida) {
        if (isFinalizada()) {
            throw new RegraNegocioException("Permanência " + id + " já foi finalizada");
        }
        Servico cadeia = servicoAte(momentoSaida);
        vaga.liberar();
        this.saida = momentoSaida;
        this.servico = cadeia;
        this.valorTotal = cadeia.custo();
    }

    /**
     * Monta a cadeia de decorators considerando a permanência até o momento
     * informado. Ex.: Manobrista(Lavagem(TarifaBase(2))).
     */
    public Servico servicoAte(LocalDateTime momento) {
        if (isFinalizada()) {
            return servico;
        }
        Servico cadeia = new TarifaBase(horasAte(momento));
        for (TipoServico tipo : extras) {
            cadeia = tipo.decorar(cadeia);
        }
        return cadeia;
    }

    /** Horas completas de permanência (frações de hora não são cobradas). */
    private int horasAte(LocalDateTime momento) {
        long horas = Duration.between(entrada, momento).toHours();
        return (int) Math.max(0, horas);
    }

    public boolean isFinalizada() {
        return saida != null;
    }

    public Long getId() {
        return id;
    }

    public Vaga getVaga() {
        return vaga;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public LocalDateTime getEntrada() {
        return entrada;
    }

    public LocalDateTime getSaida() {
        return saida;
    }

    /** Cadeia de decorators final; nula enquanto a permanência está ativa. */
    public Servico getServico() {
        return servico;
    }

    /** Valor total cobrado; nulo enquanto a permanência está ativa. */
    public Double getValorTotal() {
        return valorTotal;
    }

    public List<TipoServico> getExtras() {
        return Collections.unmodifiableList(extras);
    }
}
