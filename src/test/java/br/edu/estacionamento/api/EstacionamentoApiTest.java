package br.edu.estacionamento.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Testa os endpoints de ponta a ponta. O contexto é recriado a cada teste para
 * que os repositórios em memória comecem vazios (ids a partir de 1).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EstacionamentoApiTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void fluxoDeDemonstracaoDoSpec() throws Exception {
        // 1. Criar 3 vagas
        for (int numero = 1; numero <= 3; numero++) {
            postJson("/api/vagas", "{\"numero\": " + numero + "}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numero").value(numero))
                    .andExpect(jsonPath("$.estado").value("LIVRE"));
        }

        // 2. Reservar a vaga 1
        mvc.perform(post("/api/vagas/1/reservar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESERVADA"));

        // 3. Entrada na vaga 2 (Livre -> Ocupada)
        postJson("/api/permanencias", "{\"placa\": \"ABC1D23\", \"modelo\": \"Gol\", \"vagaId\": 2}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vagaNumero").value(2))
                .andExpect(jsonPath("$.ativa").value(true))
                .andExpect(jsonPath("$.valorTotal").value(nullValue()));

        // 4. Tentar ocupar a vaga 2 de novo -> 409
        postJson("/api/permanencias", "{\"placa\": \"XYZ9K87\", \"modelo\": \"Uno\", \"vagaId\": 2}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é possível ocupar uma vaga no estado OCUPADA"));

        // 5. Adicionar Lavagem e Manobrista
        postJson("/api/permanencias/1/servicos", "{\"tipo\": \"LAVAGEM\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicos[0]").value("LAVAGEM"));
        postJson("/api/permanencias/1/servicos", "{\"tipo\": \"MANOBRISTA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Tarifa 1h + Lavagem + Manobrista"));

        // 6. Saída: tarifa + extras somados, vaga volta a Livre
        mvc.perform(post("/api/permanencias/1/saida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false))
                .andExpect(jsonPath("$.saida").isNotEmpty())
                .andExpect(jsonPath("$.valorTotal").value(60.0));

        mvc.perform(get("/api/vagas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].estado").value("RESERVADA"))
                .andExpect(jsonPath("$[1].estado").value("LIVRE"))
                .andExpect(jsonPath("$[2].estado").value("LIVRE"));

        mvc.perform(get("/api/permanencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].placa").value("ABC1D23"));
    }

    @Test
    void liberarVagaLivreRetorna409() throws Exception {
        postJson("/api/vagas", "{\"numero\": 1}");

        mvc.perform(post("/api/vagas/1/liberar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é possível liberar uma vaga no estado LIVRE"));
    }

    @Test
    void reservarVagaReservadaRetorna409() throws Exception {
        postJson("/api/vagas", "{\"numero\": 1}");
        mvc.perform(post("/api/vagas/1/reservar"));

        mvc.perform(post("/api/vagas/1/reservar"))
                .andExpect(status().isConflict());
    }

    @Test
    void servicoDuplicadoRetorna409() throws Exception {
        postJson("/api/vagas", "{\"numero\": 1}");
        postJson("/api/permanencias", "{\"placa\": \"ABC1D23\", \"modelo\": \"Gol\", \"vagaId\": 1}");
        postJson("/api/permanencias/1/servicos", "{\"tipo\": \"LAVAGEM\"}");

        postJson("/api/permanencias/1/servicos", "{\"tipo\": \"LAVAGEM\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void vagaInexistenteRetorna404() throws Exception {
        mvc.perform(post("/api/vagas/42/reservar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Vaga 42 não encontrada"));
    }

    @Test
    void dadosInvalidosRetornam400() throws Exception {
        postJson("/api/vagas", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
        postJson("/api/vagas", "isso não é json")
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/vagas/abc/reservar"))
                .andExpect(status().isBadRequest());

        postJson("/api/vagas", "{\"numero\": 1}");
        postJson("/api/permanencias", "{\"placa\": \"\", \"modelo\": \"Gol\", \"vagaId\": 1}")
                .andExpect(status().isBadRequest());
        postJson("/api/permanencias", "{\"placa\": \"ABC1D23\", \"modelo\": \"Gol\", \"vagaId\": 1}");
        postJson("/api/permanencias/1/servicos", "{\"tipo\": \"ENCERAMENTO\"}")
                .andExpect(status().isBadRequest());
    }
}
