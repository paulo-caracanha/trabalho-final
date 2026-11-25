package com.exemplo.produtos.model;

import com.exemplo.produtos.pedido.estado.StatusPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long produtoId;
    private String produtoNome;
    private Integer quantidade;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorUnitario;

    @Column(precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal desconto;

    @Column(precision = 19, scale = 2)
    private BigDecimal total;

    private String tipoDesconto;
    private LocalDateTime criadoEm;

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    protected Pedido() {
    }

    public Pedido(Produto produto, int quantidade, String tipoDesconto, BigDecimal desconto) {
        this.produtoId = produto.getId();
        this.produtoNome = produto.getNome();
        this.quantidade = quantidade;
        this.valorUnitario = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
        this.subtotal = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
        this.tipoDesconto = tipoDesconto;
        this.desconto = desconto.setScale(2, RoundingMode.HALF_UP);
        this.total = subtotal.subtract(this.desconto);
        this.status = StatusPedido.ABERTO;
        this.criadoEm = LocalDateTime.now();
    }

    public void confirmar() {
        status.getEstado().confirmar(this);
    }

    public void cancelar() {
        status.getEstado().cancelar(this);
    }

    public void entregar() {
        status.getEstado().entregar(this);
    }

    public Long getId() {
        return id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getTipoDesconto() {
        return tipoDesconto;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }
}
