package org.wltea.analyzer.help;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.ExtendedLogger;

public class ESPluginLoggerFactory {

    private ESPluginLoggerFactory() {
    }

    public static Logger getLogger(String name) {
        return getLogger("", LogManager.getLogger(name));
    }

    public static Logger getLogger(String prefix, String name) {
        return getLogger(prefix, LogManager.getLogger(name));
    }

    public static Logger getLogger(String prefix, Class<?> clazz) {
        return getLogger(prefix, LogManager.getLogger(clazz.getName()));
    }

    public static Logger getLogger(String prefix, Logger logger) {
        return prefix != null && prefix.length() != 0 ? new PrefixPluginLogger((ExtendedLogger) logger, logger.getName(), prefix) : logger;
    }
}
