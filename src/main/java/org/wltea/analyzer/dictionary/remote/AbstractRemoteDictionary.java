package org.wltea.analyzer.dictionary.remote;

import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.configuration.ConfigurationProperties;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * AbstractRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:50
 */
public abstract class AbstractRemoteDictionary {

    enum RemoteDictionarySchema {

        HTTP("http"),
        REDIS("redis"),
        MYSQL("mysql");

        String schema;

        RemoteDictionarySchema(String schema) {
            this.schema = schema;
        }
    }

    final Configuration configuration;

    AbstractRemoteDictionary(Configuration configuration) {
        this.configuration = configuration;
    }

    ConfigurationProperties getConfigurationProperties() {
        return this.configuration.getProperties();
    }

    /**
     * 获取远程词库
     * @param uri 远程地址
     * @return words
     */
    public List<String> getRemoteWords(URI uri) {
        return this.getRemoteWords(uri.getScheme(), uri.getPath());
    }

    /**
     * 获取远程词库
     * @param schema 远程地址schema
     * @param path 远程地址path
     * @return words
     */
    public List<String> getRemoteWords(String schema, String path) {
        return Collections.emptyList();
    }

    /**
     * 重新加载词库
     * @param uri 远程地址
     */
    public void reloadRemoteDictionary(URI uri) {
        this.reloadRemoteDictionary();
    }

    /**
     * 重新加载词库
     */
    public void reloadRemoteDictionary() {

    }

    /**
     * 词典schema
     */
    abstract String schema();
}
