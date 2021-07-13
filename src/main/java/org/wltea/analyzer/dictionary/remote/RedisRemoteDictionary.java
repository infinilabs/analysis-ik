package org.wltea.analyzer.dictionary.remote;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.ConfigurationProperties;
import org.wltea.analyzer.dictionary.Dictionary;
import org.wltea.analyzer.dictionary.DictionaryType;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * RedisRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:27
 */
class RedisRemoteDictionary extends AbstractRemoteDictionary {

    private static final Logger logger = ESPluginLoggerFactory.getLogger(RedisRemoteDictionary.class.getName());

    private final StatefulRedisConnection<String, String> redisConnection;

    private final static String KEY_PREFIX = "es-ik-words";

    RedisRemoteDictionary() {
        // lettuce 默认支持connection的重连
        // 在es中并无多线程情况，使用单连接即可
        this.redisConnection = this.getRedisConnection();
    }

    @Override
    public Set<String> getRemoteWords(Dictionary dictionary,
                                      DictionaryType dictionaryType,
                                      String etymology,
                                      String domain) {
        logger.info("[Remote DictFile reloading] For etymology 'redis' domain '{}'", domain);
        RedisCommands<String, String> sync = this.redisConnection.sync();
        String key = this.getKey(dictionaryType, domain);
        List<String> words = sync.lrange(key, 0, -1);
        return new HashSet<>(words);
    }

    @Override
    public void reloadRemoteDictionary(Dictionary dictionary,
                                       DictionaryType dictionaryType,
                                       String domain) {
        logger.info("[Remote DictFile reloading] For etymology 'redis' and domain '{}'", domain);
        RedisCommands<String, String> sync = this.redisConnection.sync();
        // 当前 对应的 *-state key为true时，进行reload
        String key = this.getKey(dictionaryType, domain);
        String state = String.format("%s:state", key);
        String currentState = sync.get(state);
        logger.info("[Remote Dict File] state '{}' = '{}'", state, currentState);
        if ("true".equals(currentState)) {
            sync.set(state, "false");
            dictionary.reload(dictionaryType);
        }
    }

    @Override
    public String etymology() {
        return RemoteDictionaryEtymology.REDIS.etymology;
    }

    private String getKey(DictionaryType dictionaryType, String domain) {
        // # main-words key: es-ik-words:{domain}:main-words
        // # stop-words key: es-ik-words:{domain}:stop-words
        return String.format("%s:%s:%s", KEY_PREFIX, domain, dictionaryType.getDictName());
    }

    private StatefulRedisConnection<String, String> getRedisConnection() {
        ConfigurationProperties.Redis redis = this.getRemoteDictFile().getRedis();
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(redis.getHost())
                .withPort(redis.getPort())
                .withDatabase(redis.getDatabase());
        String username = redis.getUsername();
        String password = redis.getPassword();
        if (Objects.nonNull(username) && Objects.nonNull(password)) {
            builder.withAuthentication(redis.getUsername(), password.toCharArray());
        } else if (Objects.nonNull(password)) {
            builder.withPassword(password.toCharArray());
        }
        return RedisClient.create(builder.build()).connect();
    }
}
