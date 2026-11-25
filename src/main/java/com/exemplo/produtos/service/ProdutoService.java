package com.exemplo.produtos.service;

import com.exemplo.produtos.logger.AppLoggerComponent;
import com.exemplo.produtos.exception.RegraNegocioException;
import com.exemplo.produtos.model.Produto;
import com.exemplo.produtos.pedido.estado.StatusPedido;
import com.exemplo.produtos.repository.PedidoRepository;
import com.exemplo.produtos.repository.ProdutoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final AppLoggerComponent logger;
    private final PedidoRepository pedidos;

    public ProdutoService(ProdutoRepository repository, AppLoggerComponent logger, PedidoRepository pedidos) {
        this.repository = repository;
        this.logger = logger;
        this.pedidos = pedidos;
    }

    public Produto criar(Produto produto) {
        validar(produto);
        logger.debug("Criando produto: nome=" + produto.getNome() + ", preco=" + produto.getPreco()
                + ", quantidade=" + produto.getQuantidade());
        produto.setId(null);
        Produto salvo = repository.save(produto);
        logger.info("Produto criado com id=" + salvo.getId());
        return salvo;
    }

    public List<Produto> listar() {
        List<Produto> produtos = repository.findAll();
        logger.debug("Consulta de produtos retornou " + produtos.size() + " registro(s)");
        logger.info("Listagem de produtos concluida");
        return produtos;
    }

    public Optional<Produto> buscarPorId(Long id) {
        logger.debug("Buscando produto com id=" + id);
        Optional<Produto> produto = repository.findById(id);
        if (produto.isPresent()) {
            logger.info("Produto encontrado com id=" + id);
        } else {
            logger.erro("Produto nao encontrado com id=" + id);
        }
        return produto;
    }

    @Transactional
    public Optional<Produto> atualizar(Long id, Produto dados) {
        validar(dados);
        logger.debug("Atualizando produto com id=" + id + ": nome=" + dados.getNome()
                + ", preco=" + dados.getPreco() + ", quantidade=" + dados.getQuantidade());
        Optional<Produto> existente = repository.buscarComBloqueio(id);
        if (existente.isEmpty()) {
            logger.erro("Produto nao encontrado para atualizacao com id=" + id);
            return Optional.empty();
        }
        Produto produto = existente.get();
        produto.setNome(dados.getNome());
        produto.setDescricao(dados.getDescricao());
        produto.setPreco(dados.getPreco());
        produto.setQuantidade(dados.getQuantidade());
        Produto salvo = repository.save(produto);
        logger.info("Produto atualizado com id=" + id);
        return Optional.of(salvo);
    }

    @Transactional
    public boolean excluir(Long id) {
        logger.debug("Excluindo produto com id=" + id);
        if (repository.buscarComBloqueio(id).isEmpty()) {
            logger.erro("Produto nao encontrado para exclusao com id=" + id);
            return false;
        }
        if (pedidos.existsByProdutoIdAndStatus(id, StatusPedido.CONFIRMADO)) {
            throw new RegraNegocioException("O produto possui pedido confirmado. Entregue ou cancele o pedido antes de excluir");
        }
        repository.deleteById(id);
        logger.info("Produto excluido com id=" + id);
        return true;
    }

    private void validar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new IllegalArgumentException("Informe o nome do produto");
        }
        if (produto.getPreco() == null || !Double.isFinite(produto.getPreco()) || produto.getPreco() < 0) {
            throw new IllegalArgumentException("O preco deve ser um numero maior ou igual a zero");
        }
        if (produto.getQuantidade() == null || produto.getQuantidade() < 0) {
            throw new IllegalArgumentException("O estoque deve ser maior ou igual a zero");
        }
    }
}
