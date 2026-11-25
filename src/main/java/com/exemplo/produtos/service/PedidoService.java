package com.exemplo.produtos.service;

import com.exemplo.produtos.dto.PedidoRequest;
import com.exemplo.produtos.exception.NaoEncontradoException;
import com.exemplo.produtos.exception.RegraNegocioException;
import com.exemplo.produtos.logger.AppLoggerComponent;
import com.exemplo.produtos.model.Pedido;
import com.exemplo.produtos.model.Produto;
import com.exemplo.produtos.pedido.desconto.CalculadoraDesconto;
import com.exemplo.produtos.pedido.estado.StatusPedido;
import com.exemplo.produtos.repository.PedidoRepository;
import com.exemplo.produtos.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    private final CalculadoraDesconto calculadora;
    private final AppLoggerComponent logger;

    public PedidoService(PedidoRepository pedidos, ProdutoRepository produtos,
                         CalculadoraDesconto calculadora, AppLoggerComponent logger) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.calculadora = calculadora;
        this.logger = logger;
    }

    @Transactional
    public Pedido criar(PedidoRequest dados) {
        if (dados.getProdutoId() == null || dados.getProdutoId() <= 0) {
            throw new IllegalArgumentException("Informe um produtoId valido");
        }
        if (dados.getQuantidade() == null || dados.getQuantidade() <= 0) {
            throw new IllegalArgumentException("A quantidade do pedido deve ser maior que zero");
        }
        Produto produto = produtos.findById(dados.getProdutoId())
                .orElseThrow(() -> new NaoEncontradoException("Produto nao encontrado"));
        String tipo = dados.getTipoDesconto() == null ? "SEM_DESCONTO"
                : dados.getTipoDesconto().trim().toUpperCase(Locale.ROOT);
        BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = preco.multiply(BigDecimal.valueOf(dados.getQuantidade()));
        BigDecimal desconto = calculadora.calcular(tipo, subtotal, dados.getQuantidade());
        Pedido pedido = pedidos.save(new Pedido(produto, dados.getQuantidade(), tipo, desconto));
        logger.info("Pedido " + pedido.getId() + " criado: produto=" + produto.getId()
                + ", quantidade=" + pedido.getQuantidade() + ", desconto=" + tipo
                + ", total=" + pedido.getTotal());
        return pedido;
    }

    public List<Pedido> listar() {
        logger.debug("Listando pedidos");
        return pedidos.findAll();
    }

    public Pedido buscar(Long id) {
        logger.debug("Buscando pedido " + id);
        return pedidos.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Pedido nao encontrado"));
    }

    @Transactional
    public Pedido confirmar(Long id) {
        Pedido pedido = buscarComBloqueio(id);
        pedido.confirmar();
        Produto produto = buscarProdutoComBloqueio(pedido.getProdutoId());
        if (produto.getQuantidade() < pedido.getQuantidade()) {
            throw new RegraNegocioException("Estoque insuficiente. Disponivel: " + produto.getQuantidade());
        }
        produto.setQuantidade(produto.getQuantidade() - pedido.getQuantidade());
        produtos.save(produto);
        pedidos.save(pedido);
        logger.info("Pedido " + id + " confirmado. Estoque do produto " + produto.getId()
                + " passou para " + produto.getQuantidade());
        return pedido;
    }

    @Transactional
    public Pedido cancelar(Long id) {
        Pedido pedido = buscarComBloqueio(id);
        StatusPedido anterior = pedido.getStatus();
        pedido.cancelar();
        if (anterior == StatusPedido.CONFIRMADO) {
            Produto produto = buscarProdutoComBloqueio(pedido.getProdutoId());
            long novoEstoque = (long) produto.getQuantidade() + pedido.getQuantidade();
            if (novoEstoque > Integer.MAX_VALUE) {
                throw new RegraNegocioException("A devolucao ultrapassa o limite do estoque");
            }
            produto.setQuantidade((int) novoEstoque);
            produtos.save(produto);
            logger.info("Estoque devolvido pelo cancelamento do pedido " + id);
        }
        pedidos.save(pedido);
        logger.info("Pedido " + id + " cancelado. Estado anterior: " + anterior);
        return pedido;
    }

    @Transactional
    public Pedido entregar(Long id) {
        Pedido pedido = buscarComBloqueio(id);
        pedido.entregar();
        pedidos.save(pedido);
        logger.info("Pedido " + id + " entregue");
        return pedido;
    }

    private Pedido buscarComBloqueio(Long id) {
        return pedidos.buscarComBloqueio(id)
                .orElseThrow(() -> new NaoEncontradoException("Pedido nao encontrado"));
    }

    private Produto buscarProdutoComBloqueio(Long id) {
        return produtos.buscarComBloqueio(id)
                .orElseThrow(() -> new NaoEncontradoException("Produto do pedido nao encontrado"));
    }
}
