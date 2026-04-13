package dev.webfx.platform.console.spi.impl.sl4j;

import dev.webfx.platform.console.spi.ConsoleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruno Salmon
 */
public class Sl4jConsoleProvider implements ConsoleProvider {

    private static final Logger logger = LoggerFactory.getLogger(Sl4jConsoleProvider.class);

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(Throwable error) {
        logger.error("", error);
    }

    @Override
    public void error(String message, Throwable error) {
        logger.error(message, error);
    }

    @Override
    public void logNative(Object nativeObject) {
        log(nativeObject.toString());
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }
}
