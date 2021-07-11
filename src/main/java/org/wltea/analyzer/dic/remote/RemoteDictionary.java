package org.wltea.analyzer.dic.remote;

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

    private static final Map<String, IRemoteDictionary> REMOTE_DICTIONARY = new HashMap<>();

    private enum RemoteDictSchema {
        HTTP("http"),
        REDIS("redis"),
        MYSQL("mysql");

        String schema;

        RemoteDictSchema(String schema) {
            this.schema = schema;
        }
    }

    public static void initial(Configuration configuration) {
        REMOTE_DICTIONARY.put(RemoteDictSchema.HTTP.schema, new HttpRemoteDictionary(configuration));
        REMOTE_DICTIONARY.put(RemoteDictSchema.REDIS.schema, new RedisRemoteDictionary(configuration));
        REMOTE_DICTIONARY.put(RemoteDictSchema.MYSQL.schema, new MySQLRemoteDictionary(configuration));
        logger.info("Remote Dict Preparing...");
    }

    public static IRemoteDictionary getRemoteDictionary(URI uri) {
        String schema = uri.getScheme();
        IRemoteDictionary remoteDictionary = REMOTE_DICTIONARY.get(schema);
        logger.info("Remote Dictionary schema {}", schema);
        if (Objects.isNull(remoteDictionary)) {
            logger.error("Load Remote Error");
        }
        return remoteDictionary;
    }
}
