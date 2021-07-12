package org.wltea.analyzer.dictionary.remote;

import org.apache.logging.log4j.Logger;
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
        String schema = remoteDictionary.schema();
        REMOTE_DICTIONARY.put(schema, remoteDictionary);
        logger.info("The Remote Dictionary For schema {} is loaded!", schema);
    }

    public static void initial() {
        addRemoteDictionary(new HttpRemoteDictionary());
        addRemoteDictionary(new RedisRemoteDictionary());
        addRemoteDictionary(new MySQLRemoteDictionary());
        logger.info("Remote Dictionary Initialed");
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