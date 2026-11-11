package br.edu.estacionamento.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.estacionamento.dominio.Permanencia;
import br.edu.estacionamento.dominio.TipoServico;
import br.edu.estacionamento.servico.Servico;

/**
 * Permanência para exibição.
 *
 * <ul>
 *   <li>{@code servicos}: extras adicionados, na ordem (ex.: ["LAVAGEM"]).</li>
 *   <li>{@code descricao} / {@code valorAtual}: cobrança calculada pela cadeia
 *       de decorators. Na permanência ativa é parcial (até agora); na
 *       finalizada é a definitiva.</li>
 *   <li>{@code valorTotal}: valor final cobrado; nulo enquanto ativa.</li>
 * </ul>
 */
public record PermanenciaResponse(
        Long id,
        Long vagaId,
        int vagaNumero,
        String placa,
        String modelo,
        LocalDateTime entrada,
        LocalDateTime saida,
        boolean ativa,
        List<String> servicos,
        String descricao,
        double valorAtual,
        Double valorTotal) {

    public static PermanenciaResponse de(Permanencia p, LocalDateTime agora) {
        Servico servico = p.servicoAte(agora);
        return new PermanenciaResponse(
                p.getId(),
                p.getVaga().getId(),
                p.getVaga().getNumero(),
                p.getVeiculo().placa(),
                p.getVeiculo().modelo(),
                p.getEntrada(),
                p.getSaida(),
                !p.isFinalizada(),
                p.getExtras().stream().map(TipoServico::name).toList(),
                servico.descricao(),
                servico.custo(),
                p.getValorTotal());
    }
}
