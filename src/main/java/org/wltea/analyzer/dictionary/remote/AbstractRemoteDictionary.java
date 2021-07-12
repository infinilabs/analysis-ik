package org.wltea.analyzer.dictionary.remote;

import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.configuration.ConfigurationProperties;
import org.wltea.analyzer.dictionary.Dictionary;
import org.wltea.analyzer.dictionary.DictionaryType;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

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

    ConfigurationProperties getConfigurationProperties() {
        return Configuration.getProperties();
    }

    /**
     * 获取远程词库
     * @param dictionary 词典
     * @param dictionaryType 词典类型
     * @param uri 远程地址
     * @return words
     */
    public Set<String> getRemoteWords(Dictionary dictionary, DictionaryType dictionaryType, URI uri) {
        return this.getRemoteWords(dictionary, dictionaryType, uri.getScheme(), uri.getAuthority());
    }

    /**
     * 获取远程词库
     * @param dictionary 词典
     * @param dictionaryType 词典类型
     * @param schema 远程地址schema
     * @param authority 远程地址path
     * @return words
     */
    public Set<String> getRemoteWords(Dictionary dictionary, DictionaryType dictionaryType, String schema, String authority) {
        return Collections.emptySet();
    }

    /**
     * 重新加载词库
     * @param dictionary 词典
     * @param dictionaryType 词典类型
     * @param uri 远程地址
     */
    public void reloadRemoteDictionary(Dictionary dictionary,  DictionaryType dictionaryType, URI uri) {
        this.reloadRemoteDictionary(dictionary, dictionaryType, uri.getAuthority());
    }

    /**
     * 重新加载词库
     * @param dictionary 词典
     * @param dictionaryType 词典类型
     * @param authority redis key 或 or db table name
     */
    public void reloadRemoteDictionary(Dictionary dictionary, DictionaryType dictionaryType, String authority) {

    }

    /**
     * 词典schema
     */
    abstract String schema();
}
