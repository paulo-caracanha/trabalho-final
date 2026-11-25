package com.exemplo.produtos.pedido.desconto;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class SemDesconto implements DescontoStrategy {

    @Override
    public String getTipo() {
        return "SEM_DESCONTO";
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, int quantidade) {
        return BigDecimal.ZERO;
    }
}
