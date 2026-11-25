package com.exemplo.produtos.pedido.desconto;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraDesconto {

    private final List<DescontoStrategy> estrategias;

    public CalculadoraDesconto(List<DescontoStrategy> estrategias) {
        this.estrategias = estrategias;
    }

    public BigDecimal calcular(String tipo, BigDecimal subtotal, int quantidade) {
        for (DescontoStrategy estrategia : estrategias) {
            if (estrategia.getTipo().equals(tipo)) {
                return estrategia.calcular(subtotal, quantidade);
            }
        }
        throw new IllegalArgumentException("Tipo de desconto invalido: " + tipo);
    }
}
