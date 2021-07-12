package org.wltea.analyzer.dictionary.remote;

import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.dictionary.DictionaryType;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.util.List;

/**
 * RedisRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:27
 */
class RedisRemoteDictionary extends AbstractRemoteDictionary {

    private static final Logger logger = ESPluginLoggerFactory.getLogger(RedisRemoteDictionary.class.getName());

    RedisRemoteDictionary(Configuration configuration) {
        super(configuration);
    }

    @Override
    public List<String> getRemoteWords(DictionaryType dictionaryType, String schema, String authority) {
        logger.info("[Remote DictFile reloading] For schema 'redis' path {}", authority);
        return null;
    }

    @Override
    public void reloadRemoteDictionary(DictionaryType dictionaryType, String authority) {
        logger.info("[Remote DictFile reloading] For schema 'redis'");
    }

    @Override
    public String schema() {
        return RemoteDictionarySchema.REDIS.schema;
    }
}
