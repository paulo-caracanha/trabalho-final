package com.exemplo.produtos.pedido;

import com.exemplo.produtos.exception.RegraNegocioException;
import com.exemplo.produtos.pedido.desconto.DescontoAtacado;
import com.exemplo.produtos.pedido.desconto.DescontoCompraMaior;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DescontoTest {

    @Test
    void atacadoExigeDezUnidades() {
        DescontoAtacado desconto = new DescontoAtacado();
        assertThrows(RegraNegocioException.class, () -> desconto.calcular(new BigDecimal("1000"), 9));
        assertEquals(new BigDecimal("100.00"), desconto.calcular(new BigDecimal("1000"), 10));
    }

    @Test
    void compraMaiorExigeQuinhentosReais() {
        DescontoCompraMaior desconto = new DescontoCompraMaior();
        assertThrows(RegraNegocioException.class, () -> desconto.calcular(new BigDecimal("499.99"), 1));
        assertEquals(new BigDecimal("25.00"), desconto.calcular(new BigDecimal("500.00"), 1));
    }

    @Test
    void descontoArredondaParaCentavos() {
        assertEquals(new BigDecimal("50.01"), new DescontoAtacado().calcular(new BigDecimal("500.05"), 10));
    }
}
