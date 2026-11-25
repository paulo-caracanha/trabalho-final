package com.exemplo.produtos.logger;

import org.springframework.stereotype.Component;

@Component
public class AppLoggerComponent {

    private final AppLogger logger = AppLogger.getInstance();

    public void debug(String mensagem) {
        logger.debug(mensagem);
    }

    public void info(String mensagem) {
        logger.info(mensagem);
    }

    public void erro(String mensagem) {
        logger.erro(mensagem);
    }
}
