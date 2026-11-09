package br.edu.estacionamento.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServicoDecoratorTest {

    private static final double DELTA = 0.001;

    @Test
    void semExtrasCobraApenasTarifaBase() {
        Servico servico = new TarifaBase(2);

        assertEquals("Tarifa 2h", servico.descricao());
        assertEquals(20.0, servico.custo(), DELTA);
    }

    @Test
    void comLavagemSomaValorDaLavagem() {
        Servico servico = new Lavagem(new TarifaBase(2));

        assertEquals("Tarifa 2h + Lavagem", servico.descricao());
        assertEquals(50.0, servico.custo(), DELTA);
    }

    @Test
    void comManobristaSomaValorDoManobrista() {
        Servico servico = new Manobrista(new TarifaBase(2));

        assertEquals("Tarifa 2h + Manobrista", servico.descricao());
        assertEquals(40.0, servico.custo(), DELTA);
    }

    @Test
    void comDoisExtrasSomaTudo() {
        Servico servico = new Manobrista(new Lavagem(new TarifaBase(2)));

        assertEquals("Tarifa 2h + Lavagem + Manobrista", servico.descricao());
        assertEquals(70.0, servico.custo(), DELTA);
    }

    @Test
    void ordemDiferenteMudaDescricaoMasNaoOCusto() {
        Servico lavagemPrimeiro = new Manobrista(new Lavagem(new TarifaBase(3)));
        Servico manobristaPrimeiro = new Lavagem(new Manobrista(new TarifaBase(3)));

        assertEquals("Tarifa 3h + Lavagem + Manobrista", lavagemPrimeiro.descricao());
        assertEquals("Tarifa 3h + Manobrista + Lavagem", manobristaPrimeiro.descricao());
        assertEquals(lavagemPrimeiro.custo(), manobristaPrimeiro.custo(), DELTA);
        assertEquals(80.0, manobristaPrimeiro.custo(), DELTA);
    }

    @Test
    void mesmoExtraPodeSerAplicadoMaisDeUmaVez() {
        Servico servico = new Lavagem(new Lavagem(new TarifaBase(1)));

        assertEquals("Tarifa 1h + Lavagem + Lavagem", servico.descricao());
        assertEquals(70.0, servico.custo(), DELTA);
    }

    @Test
    void tarifaBaseNaoAceitaHorasNegativas() {
        assertThrows(IllegalArgumentException.class, () -> new TarifaBase(-1));
    }

    @Test
    void decoratorNaoAceitaServicoNulo() {
        assertThrows(IllegalArgumentException.class, () -> new Lavagem(null));
    }
}
