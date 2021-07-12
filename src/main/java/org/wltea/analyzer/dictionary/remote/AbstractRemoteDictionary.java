package org.wltea.analyzer.dictionary.remote;

import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.configuration.ConfigurationProperties;
import org.wltea.analyzer.dictionary.DictionaryType;

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
     * @param dictionaryType 词典类型
     * @param uri 远程地址
     * @return words
     */
    public List<String> getRemoteWords(DictionaryType dictionaryType, URI uri) {
        return this.getRemoteWords(dictionaryType, uri.getScheme(), uri.getAuthority());
    }

    /**
     * 获取远程词库
     * @param dictionaryType 词典类型
     * @param schema 远程地址schema
     * @param authority 远程地址path
     * @return words
     */
    public List<String> getRemoteWords(DictionaryType dictionaryType, String schema, String authority) {
        return Collections.emptyList();
    }

    /**
     * 重新加载词库
     * @param dictionaryType 词典类型
     * @param uri 远程地址
     */
    public void reloadRemoteDictionary(DictionaryType dictionaryType, URI uri) {
        this.reloadRemoteDictionary(dictionaryType, uri.getAuthority());
    }

    /**
     * 重新加载词库
     */
    public void reloadRemoteDictionary(DictionaryType dictionaryType, String authority) {

    }

    /**
     * 词典schema
     */
    abstract String schema();
}
