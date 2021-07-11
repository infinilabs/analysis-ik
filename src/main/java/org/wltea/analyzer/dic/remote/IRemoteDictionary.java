package org.wltea.analyzer.dic.remote;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * IRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 15:39
 */
public interface IRemoteDictionary {

    /**
     * 获取远程词库
     * @param uri 远程地址
     * @return words
     */
    default List<String> getRemoteWords(URI uri) {
        return this.getRemoteWords(uri.getScheme(), uri.getPath());
    }

    /**
     * 获取远程词库
     * @param schema 远程地址schema
     * @param path 远程地址path
     * @return words
     */
    default List<String> getRemoteWords(String schema, String path) {
        return Collections.emptyList();
    }

    /**
     * 重新加载词库
     * @param uri 远程地址
     */
    default void reloadRemoteDictionary(URI uri) {
        this.reloadRemoteDictionary();
    }

    /**
     * 重新加载词库
     */
    default void reloadRemoteDictionary() {

    }
}
