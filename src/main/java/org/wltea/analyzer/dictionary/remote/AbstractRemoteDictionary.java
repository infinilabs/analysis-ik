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

    ConfigurationProperties.Remote getRemoteDictFile() {
        return Configuration.getProperties().getDict().getRemote();
    }

    /**
     * 获取远程词库
     * @param dictionary 词典
     * @param dictionaryType 词典类型
     * @param domainUri 领域词源Uri
     * @return words
     */
    public Set<String> getRemoteWords(Dictionary dictionary,
                                      DictionaryType dictionaryType,
                                      URI domainUri) {
        return this.getRemoteWords(dictionary,
                dictionaryType,
                domainUri.getScheme(),
                domainUri.getAuthority());
    }

    /**
     * 获取远程词库
     * @param dictionary 词典
     * @param dictionaryType 词典类型
     * @param etymology 词源
     * @param domain 领域
     * @return words
     */
    public Set<String> getRemoteWords(Dictionary dictionary,
                                      DictionaryType dictionaryType,
                                      String etymology,
                                      String domain) {
        return Collections.emptySet();
    }

    /**
     * 重新加载词库
     * @param dictionary 词典
     * @param dictionaryType 词典类型
     * @param domainUri 领域词源Uri
     */
    public void reloadRemoteDictionary(Dictionary dictionary,
                                       DictionaryType dictionaryType,
                                       URI domainUri) {
        this.reloadRemoteDictionary(dictionary,
                dictionaryType,
                domainUri.getAuthority());
    }

    /**
     * 重新加载词库
     * @param dictionary 词典
     * @param dictionaryType 词典类型
     * @param domain 领域
     */
    public void reloadRemoteDictionary(Dictionary dictionary,
                                       DictionaryType dictionaryType,
                                       String domain) {

    }

    /**
     * 词典词源
     */
    abstract String etymology();
}
