package org.wltea.analyzer.dictionary.remote;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.Configuration;
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

    RedisRemoteDictionary(Configuration configuration) {
        super(configuration);
        // lettuce 默认支持connection的重连
        // 在es中并无多线程情况，使用单连接即可
        this.redisConnection = this.getRedisConnection();
    }

    @Override
    public Set<String> getRemoteWords(DictionaryType dictionaryType, String schema, String authority) {
        logger.info("[Remote DictFile reloading] For schema 'redis' path {}", authority);
        RedisCommands<String, String> sync = this.redisConnection.sync();
        List<String> words = sync.lrange(authority, 0, -1);
        return new HashSet<>(words);
    }

    @Override
    public void reloadRemoteDictionary(DictionaryType dictionaryType, String authority) {
        logger.info("[Remote DictFile reloading] For schema 'redis'");
        RedisCommands<String, String> sync = this.redisConnection.sync();
        // 当前 对应的 *-state key为true时，进行reload
        String state = String.format("%s-state", authority);
        sync.multi();
        String currentState = sync.get(state);
        logger.info("[Remote Dict File] state {} = {}", state, currentState);
        boolean reload = false;
        if ("true".equals(currentState)) {
            sync.set(state, "false");
            reload = true;
        }
        TransactionResult exec = sync.exec();
        for (Object ret : exec) {
            logger.info("Redis TransactionResult {}", ret);
            if ("OK".equals(ret.toString()) && reload) {
                Dictionary.getDictionary().reload(dictionaryType);
            }
            break;
        }
    }

    @Override
    public String schema() {
        return RemoteDictionarySchema.REDIS.schema;
    }

    private StatefulRedisConnection<String, String> getRedisConnection() {
        ConfigurationProperties.Redis redis = this.getConfigurationProperties().getRedis();
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
