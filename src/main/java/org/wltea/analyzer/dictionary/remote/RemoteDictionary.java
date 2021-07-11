package org.wltea.analyzer.dictionary.remote;

import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * RemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:46
 */
public final class RemoteDictionary {

    private static final Logger logger = ESPluginLoggerFactory.getLogger(RemoteDictionary.class.getName());

    private static final Map<String, AbstractRemoteDictionary> REMOTE_DICTIONARY = new HashMap<>();

    private static void addRemoteDictionary(AbstractRemoteDictionary remoteDictionary) {
        REMOTE_DICTIONARY.put(remoteDictionary.schema(), remoteDictionary);
    }

    public static void initial(Configuration configuration) {
        addRemoteDictionary(new HttpRemoteDictionary(configuration));
        addRemoteDictionary(new RedisRemoteDictionary(configuration));
        addRemoteDictionary(new MySQLRemoteDictionary(configuration));
        logger.info("Remote Dictionary Preparing...");
    }

    public static AbstractRemoteDictionary getRemoteDictionary(URI uri) {
        String schema = uri.getScheme();
        logger.info("Remote Dictionary schema {}", schema);
        AbstractRemoteDictionary remoteDictionary = REMOTE_DICTIONARY.get(schema);
        if (Objects.isNull(remoteDictionary)) {
            logger.error("Load Remote Dictionary Error");
        }
        return remoteDictionary;
    }
}
