package org.wltea.analyzer.dictionary.remote;

import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.util.List;

/**
 * MySQLRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:27
 */
class MySQLRemoteDictionary extends AbstractRemoteDictionary {

    private static final Logger logger = ESPluginLoggerFactory.getLogger(MySQLRemoteDictionary.class.getName());

    MySQLRemoteDictionary(Configuration configuration) {
        super(configuration);
    }

    @Override
    public List<String> getRemoteWords(String schema, String path) {
        logger.info("[Remote DictFile Loading] For schema 'mysql' and path {}", path);
        return null;
    }

    @Override
    public void reloadRemoteDictionary() {
        logger.info("[Remote DictFile Reloading] For schema 'mysql'");
    }

    @Override
    public String schema() {
        return RemoteDictionarySchema.MYSQL.schema;
    }
}
