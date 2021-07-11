package org.wltea.analyzer.dic.remote;

import org.wltea.analyzer.configuration.Configuration;

import java.util.List;

/**
 * RedisRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:27
 */
public class RedisRemoteDictionary extends AbstractRemoteDictionary implements IRemoteDictionary {

    public RedisRemoteDictionary(Configuration configuration) {
        super(configuration);
    }

    @Override
    public List<String> getRemoteWords(String schema, String path) {
        return null;
    }

    @Override
    public void reloadRemoteDictionary() {

    }
}
