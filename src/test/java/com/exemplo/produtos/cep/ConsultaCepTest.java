package com.exemplo.produtos.cep;

import com.exemplo.produtos.logger.AppLoggerComponent;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConsultaCepTest {

    private final AppLoggerComponent logger = new AppLoggerComponent();

    @Test
    void proxyConsultaUmaVezEReutilizaCache() {
        ConsultaCep provedor = mock(ConsultaCep.class);
        Endereco endereco = new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP");
        when(provedor.buscar("01001000")).thenReturn(endereco);
        ConsultaCepProxy proxy = new ConsultaCepProxy(provedor, logger);
        assertSame(endereco, proxy.buscar("01001000"));
        assertSame(endereco, proxy.buscar("01001000"));
        verify(provedor, times(1)).buscar("01001000");
    }

    @Test
    void proxyBloqueiaFormatoInvalidoSemConsultarProvedor() {
        ConsultaCep provedor = mock(ConsultaCep.class);
        ConsultaCepProxy proxy = new ConsultaCepProxy(provedor, logger);
        assertThrows(IllegalArgumentException.class, () -> proxy.buscar("123"));
        assertThrows(IllegalArgumentException.class, () -> proxy.buscar(null));
        verifyNoInteractions(provedor);
    }

    @Test
    void viaCepConverteCamposParaEndereco() {
        ViaCepAdapter adapter = new ViaCepAdapter(logger);
        RestTemplate client = (RestTemplate) ReflectionTestUtils.getField(adapter, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(client);
        server.expect(requestTo("https://viacep.com.br/ws/01001000/json/"))
                .andRespond(withSuccess("{\"cep\":\"01001-000\",\"logradouro\":\"Praca da Se\",\"bairro\":\"Se\",\"localidade\":\"Sao Paulo\",\"uf\":\"SP\"}", MediaType.APPLICATION_JSON));
        Endereco endereco = adapter.buscar("01001000");
        assertEquals("Praca da Se", endereco.getRua());
        assertEquals("Sao Paulo", endereco.getCidade());
        assertEquals("SP", endereco.getEstado());
        server.verify();
    }

    @Test
    void brasilApiConverteCamposParaEndereco() {
        BrasilApiAdapter adapter = new BrasilApiAdapter(logger);
        RestTemplate client = (RestTemplate) ReflectionTestUtils.getField(adapter, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(client);
        server.expect(requestTo("https://brasilapi.com.br/api/cep/v1/01001000"))
                .andRespond(withSuccess("{\"cep\":\"01001000\",\"street\":\"Praca da Se\",\"neighborhood\":\"Se\",\"city\":\"Sao Paulo\",\"state\":\"SP\"}", MediaType.APPLICATION_JSON));
        Endereco endereco = adapter.buscar("01001000");
        assertEquals("Praca da Se", endereco.getRua());
        assertEquals("Sao Paulo", endereco.getCidade());
        assertEquals("SP", endereco.getEstado());
        server.verify();
    }

    @Test
    void cepNaoEncontradoNaoEntraNoCache() {
        ConsultaCep provedor = mock(ConsultaCep.class);
        ConsultaCepProxy proxy = new ConsultaCepProxy(provedor, logger);
        assertNull(proxy.buscar("00000000"));
        assertNull(proxy.buscar("00000000"));
        verify(provedor, times(2)).buscar("00000000");
    }
}
