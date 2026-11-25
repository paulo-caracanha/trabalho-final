package com.exemplo.produtos;

import com.exemplo.produtos.dto.PedidoRequest;
import com.exemplo.produtos.exception.RegraNegocioException;
import com.exemplo.produtos.model.Produto;
import com.exemplo.produtos.repository.PedidoRepository;
import com.exemplo.produtos.repository.ProdutoRepository;
import com.exemplo.produtos.service.PedidoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testes;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PedidosApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ProdutoRepository produtos;

    @Autowired
    private PedidoRepository pedidos;

    @Autowired
    private PedidoService service;

    @BeforeEach
    void limparBanco() {
        pedidos.deleteAll();
        produtos.deleteAll();
    }

    @Test
    void confirmaECancelaDevolvendoOEstoqueUmaVez() throws Exception {
        long produtoId = produto(100.0, 20);
        long pedidoId = pedido(produtoId, 10, "ATACADO");
        mvc.perform(get("/pedidos/" + pedidoId))
                .andExpect(jsonPath("$.subtotal").value(1000.0))
                .andExpect(jsonPath("$.desconto").value(100.0))
                .andExpect(jsonPath("$.total").value(900.0));
        assertEquals(20, estoque(produtoId));
        mvc.perform(post("/pedidos/" + pedidoId + "/confirmar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMADO"));
        assertEquals(10, estoque(produtoId));
        mvc.perform(post("/pedidos/" + pedidoId + "/confirmar")).andExpect(status().isConflict());
        assertEquals(10, estoque(produtoId));
        mvc.perform(post("/pedidos/" + pedidoId + "/cancelar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        mvc.perform(post("/pedidos/" + pedidoId + "/cancelar")).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + pedidoId + "/confirmar")).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + pedidoId + "/entregar")).andExpect(status().isConflict());
        assertEquals(20, estoque(produtoId));
    }

    @Test
    void entregaSomentePedidoConfirmadoENaoPermiteAlterarDepois() throws Exception {
        long produtoId = produto(30.0, 5);
        long pedidoId = pedido(produtoId, 2, "SEM_DESCONTO");
        mvc.perform(post("/pedidos/" + pedidoId + "/entregar")).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + pedidoId + "/confirmar")).andExpect(status().isOk());
        mvc.perform(post("/pedidos/" + pedidoId + "/entregar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENTREGUE"));
        mvc.perform(post("/pedidos/" + pedidoId + "/cancelar")).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + pedidoId + "/confirmar")).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + pedidoId + "/entregar")).andExpect(status().isConflict());
        assertEquals(3, estoque(produtoId));
    }

    @Test
    void cancelarPedidoAbertoNaoAlteraEstoque() throws Exception {
        long produtoId = produto(10.0, 8);
        long pedidoId = pedido(produtoId, 4, "SEM_DESCONTO");
        mvc.perform(post("/pedidos/" + pedidoId + "/cancelar")).andExpect(status().isOk());
        assertEquals(8, estoque(produtoId));
    }

    @Test
    void estoqueInsuficienteMantemPedidoAberto() throws Exception {
        long produtoId = produto(10.0, 2);
        long pedidoId = pedido(produtoId, 3, "SEM_DESCONTO");
        mvc.perform(post("/pedidos/" + pedidoId + "/confirmar"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensagem").exists());
        mvc.perform(get("/pedidos/" + pedidoId)).andExpect(jsonPath("$.status").value("ABERTO"));
        assertEquals(2, estoque(produtoId));
    }

    @Test
    void validaQuantidadeProdutoETipoDeDesconto() throws Exception {
        long produtoId = produto(10.0, 20);
        for (String corpo : new String[] {
                "{}",
                "{\"produtoId\":" + produtoId + ",\"quantidade\":0}",
                "{\"produtoId\":" + produtoId + ",\"quantidade\":-1}",
                "{\"produtoId\":" + produtoId + ",\"quantidade\":1.5}",
                "{\"produtoId\":" + produtoId + ",\"quantidade\":1,\"tipoDesconto\":\"INVALIDO\"}"
        }) {
            mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"produtoId\":999999,\"quantidade\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/pedidos/999999")).andExpect(status().isNotFound());
        mvc.perform(post("/pedidos/999999/confirmar")).andExpect(status().isNotFound());
        assertEquals(0, pedidos.count());
    }

    @Test
    void descontoOpcionalUsaSemDesconto() throws Exception {
        long produtoId = produto(15.5, 5);
        mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"produtoId\":" + produtoId + ",\"quantidade\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoDesconto").value("SEM_DESCONTO"))
                .andExpect(jsonPath("$.total").value(31.0));
    }

    @Test
    void bloqueiaDescontoQuandoCompraNaoAtendeARegra() throws Exception {
        long produtoId = produto(10.0, 20);
        for (String tipo : new String[] {"ATACADO", "COMPRA_MAIOR"}) {
            mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dados(produtoId, 1, tipo))))
                    .andExpect(status().isConflict());
        }
        assertEquals(0, pedidos.count());
    }

    @Test
    void preservaPrecoDoPedidoMesmoComMudancaNoProduto() throws Exception {
        long produtoId = produto(100.0, 20);
        long pedidoId = pedido(produtoId, 5, "COMPRA_MAIOR");
        mvc.perform(put("/produtos/" + produtoId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Mouse novo\",\"preco\":200,\"quantidade\":20}"))
                .andExpect(status().isOk());
        mvc.perform(post("/pedidos/" + pedidoId + "/confirmar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoNome").value("Mouse"))
                .andExpect(jsonPath("$.valorUnitario").value(100.0))
                .andExpect(jsonPath("$.desconto").value(25.0))
                .andExpect(jsonPath("$.total").value(475.0));
        mvc.perform(get("/pedidos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void bloqueiaExclusaoDeProdutoComPedidoConfirmado() throws Exception {
        long produtoId = produto(25.0, 5);
        long pedidoId = pedido(produtoId, 2, "SEM_DESCONTO");
        mvc.perform(post("/pedidos/" + pedidoId + "/confirmar")).andExpect(status().isOk());
        mvc.perform(delete("/produtos/" + produtoId)).andExpect(status().isConflict());
        mvc.perform(post("/pedidos/" + pedidoId + "/cancelar")).andExpect(status().isOk());
        mvc.perform(delete("/produtos/" + produtoId)).andExpect(status().isNoContent());
        mvc.perform(get("/pedidos/" + pedidoId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoNome").value("Mouse"));
    }

    @Test
    void crudDeProdutosContinuaFuncionando() throws Exception {
        String resposta = mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teclado\",\"descricao\":\"USB\",\"preco\":89.9,\"quantidade\":10}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(resposta).get("id").asLong();
        mvc.perform(get("/produtos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/produtos/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Teclado"));
        mvc.perform(put("/produtos/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teclado novo\",\"preco\":100,\"quantidade\":15}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(15));
        mvc.perform(delete("/produtos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/produtos/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/produtos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void rejeitaProdutoSemNomePrecoOuEstoqueValido() throws Exception {
        for (String corpo : new String[] {
                "{}",
                "{\"nome\":\"Mouse\",\"preco\":-1,\"quantidade\":1}",
                "{\"nome\":\"Mouse\",\"preco\":1,\"quantidade\":-1}"
        }) {
            mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void cepInvalidoContinuaSendoBloqueado() throws Exception {
        mvc.perform(get("/cep/123")).andExpect(status().isBadRequest());
    }

    @Test
    void pedidosSimultaneosNaoVendemMaisQueOEstoque() throws Exception {
        long produtoId = produto(10.0, 5);
        long primeiro = pedido(produtoId, 4, "SEM_DESCONTO");
        long segundo = pedido(produtoId, 4, "SEM_DESCONTO");
        CountDownLatch largada = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var resultado1 = executor.submit(() -> confirmarJunto(primeiro, largada));
            var resultado2 = executor.submit(() -> confirmarJunto(segundo, largada));
            largada.countDown();
            assertEquals(1, resultado1.get(20, TimeUnit.SECONDS) + resultado2.get(20, TimeUnit.SECONDS));
        }
        assertEquals(1, estoque(produtoId));
        assertEquals(1, pedidos.findAll().stream().filter(p -> p.getStatus().name().equals("CONFIRMADO")).count());
    }

    @Test
    void duasConfirmacoesSimultaneasDoMesmoPedidoBaixamUmaVez() throws Exception {
        long produtoId = produto(10.0, 20);
        long pedidoId = pedido(produtoId, 4, "SEM_DESCONTO");
        CountDownLatch largada = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var resultado1 = executor.submit(() -> confirmarJunto(pedidoId, largada));
            var resultado2 = executor.submit(() -> confirmarJunto(pedidoId, largada));
            largada.countDown();
            assertEquals(1, resultado1.get(20, TimeUnit.SECONDS) + resultado2.get(20, TimeUnit.SECONDS));
        }
        assertEquals(16, estoque(produtoId));
    }

    private int confirmarJunto(long pedidoId, CountDownLatch largada) throws InterruptedException {
        assertTrue(largada.await(10, TimeUnit.SECONDS));
        try {
            service.confirmar(pedidoId);
            return 1;
        } catch (RegraNegocioException e) {
            return 0;
        }
    }

    private long produto(double preco, int quantidade) {
        Produto produto = new Produto();
        produto.setNome("Mouse");
        produto.setPreco(preco);
        produto.setQuantidade(quantidade);
        return produtos.save(produto).getId();
    }

    private PedidoRequest dados(long produtoId, int quantidade, String tipo) {
        PedidoRequest dados = new PedidoRequest();
        dados.setProdutoId(produtoId);
        dados.setQuantidade(quantidade);
        dados.setTipoDesconto(tipo);
        return dados;
    }

    private long pedido(long produtoId, int quantidade, String tipo) throws Exception {
        String resposta = mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dados(produtoId, quantidade, tipo))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ABERTO"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(resposta);
        return json.get("id").asLong();
    }

    private int estoque(long produtoId) {
        return produtos.findById(produtoId).orElseThrow().getQuantidade();
    }
}
