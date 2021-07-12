package org.wltea.analyzer.dictionary.remote;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.configuration.ConfigurationProperties;
import org.wltea.analyzer.dictionary.Dictionary;
import org.wltea.analyzer.dictionary.DictionaryType;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * MySQLRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:27
 */
class MySQLRemoteDictionary extends AbstractRemoteDictionary {

    private static final Logger logger = ESPluginLoggerFactory.getLogger(MySQLRemoteDictionary.class.getName());

    private final HikariDataSource dataSource;

    MySQLRemoteDictionary(Configuration configuration) {
        super(configuration);
        this.dataSource = this.initDataSource();
    }

    @Override
    public Set<String> getRemoteWords(DictionaryType dictionaryType, String schema, String authority) {
        logger.info("[Remote DictFile Loading] For schema 'mysql' and tableName {}", authority);
        Set<String> words = new HashSet<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement(String.format("SELECT word FROM %s", authority));
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String word = resultSet.getString("word");
                words.add(word);
            }
            logger.info("[Remote DictFile Loading] append {} words.", words.size());
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(" [Remote DictFile Loading] Error {}", e.getLocalizedMessage());
        } finally {
            this.closeResources(connection, statement, resultSet);
        }
        return words;
    }

    @Override
    public void reloadRemoteDictionary(DictionaryType dictionaryType, String authority) {
        logger.info("[Remote DictFile Reloading] For schema 'mysql' and path[TableName] {}", authority);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.dataSource.getConnection();
            String sql = String.format("SELECT MAX(dict.id) max_id, seq.current_id current_id FROM %s dict, ik_sequence seq", authority);
            sql = String.format("%s WHERE seq.dictionary = ? LIMIT 1", sql);
            statement = connection.prepareStatement(sql);
            statement.setString(1, authority);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                logger.info("Cannot find the `ik_sequence` and dictionary {} data", authority);
                return;
            }
            long maxId = resultSet.getLong("max_id");
            long currentId = resultSet.getLong("current_id");
            logger.info("[Remote DictFile] maxId {} currentId {}", maxId, currentId);
            if (maxId != currentId) {
                // 更新currentId
                sql = String.format("current_id = %s WHERE dictionary = '%s'", maxId, authority);
                sql = String.format("UPDATE ik_sequence SET %s", sql);
                logger.info("sql {}", sql);
                statement.execute(sql);
                Dictionary.getDictionary().reload(dictionaryType);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(" [Remote DictFile Reloading] Error {}", e.getLocalizedMessage());
        } finally {
            this.closeResources(connection, statement, resultSet);
        }
    }

    @Override
    public String schema() {
        return RemoteDictionarySchema.MYSQL.schema;
    }

    private HikariDataSource initDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        ConfigurationProperties.MySQL mysql = this.getConfigurationProperties().getMysql();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl(mysql.getUrl());
        dataSource.setUsername(mysql.getUsername());
        dataSource.setPassword(mysql.getPassword());
        return dataSource;
    }

    private void closeResources(Connection connection, Statement statement, ResultSet resultSet) {
        try {
            if (Objects.nonNull(connection)) {
                connection.close();
            }
            if (Objects.nonNull(statement)) {
                statement.close();
            }
            if (Objects.nonNull(resultSet)) {
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("[Remote DictFile 'mysql'] closeResources error {}", e.getLocalizedMessage());
        }
    }
}
