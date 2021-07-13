package org.wltea.analyzer.dictionary.remote;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.ConfigurationProperties;
import org.wltea.analyzer.dictionary.Dictionary;
import org.wltea.analyzer.dictionary.DictionaryType;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.sql.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * MySQLRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:27
 */
class MySQLRemoteDictionary extends AbstractRemoteDictionary {

    private static final Logger logger = ESPluginLoggerFactory.getLogger(MySQLRemoteDictionary.class.getName());

    private final HikariDataSource dataSource;

    MySQLRemoteDictionary() {
        this.dataSource = this.initDataSource();
    }

    @Override
    public Set<String> getRemoteWords(org.wltea.analyzer.dictionary.Dictionary dictionary,
                                      DictionaryType dictionaryType,
                                      String etymology,
                                      String domain) {
        logger.info("[Remote DictFile Loading] For etymology 'mysql' and domain '{}'", domain);
        Set<String> words = new HashSet<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement("SELECT word FROM ik_words WHERE domain = ? AND word_type = ?");
            statement.setString(1, domain);
            statement.setInt(2, dictionaryType.getType());
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String word = resultSet.getString("word");
                words.add(word);
            }
            logger.info("[Remote DictFile Loading] append {} words.", words.size());
        } catch (SQLException e) {
            logger.error(" [Remote DictFile Loading] error =>", e);
        } finally {
            this.closeResources(connection, statement, resultSet);
        }
        return words;
    }

    @Override
    public void reloadRemoteDictionary(Dictionary dictionary,
                                       DictionaryType dictionaryType,
                                       String domain) {
        logger.info("[Remote DictFile Reloading] For etymology 'mysql' and domain '{}'", domain);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.dataSource.getConnection();
            String sql = "SELECT MAX(words.id) max_id, seq.current_id current_id FROM ik_words words, ik_sequence seq WHERE seq.domain = ? LIMIT 1";
            statement = connection.prepareStatement(sql);
            statement.setString(1, domain);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                logger.info("Cannot find the `ik_sequence` and dictionary '{}' data", domain);
                return;
            }
            long maxId = resultSet.getLong("max_id");
            long currentId = resultSet.getLong("current_id");
            logger.info("[Remote DictFile] maxId '{}' currentId '{}'", maxId, currentId);
            if (maxId != currentId) {
                // 更新currentId
                sql = String.format("current_id = %s WHERE domain = '%s'", maxId, domain);
                sql = String.format("UPDATE ik_sequence SET %s", sql);
                logger.info("sql '{}'", sql);
                statement.execute(sql);
                dictionary.reload(dictionaryType);
            }
        } catch (SQLException e) {
            logger.error(" [Remote DictFile Reloading] error =>", e);
        } finally {
            this.closeResources(connection, statement, resultSet);
        }
    }

    @Override
    public String etymology() {
        return RemoteDictionaryEtymology.MYSQL.etymology;
    }

    private HikariDataSource initDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        ConfigurationProperties.MySQL mysql = this.getRemoteDictFile().getMysql();
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
            logger.error("[Remote DictFile 'mysql'] closeResources error =>", e);
        }
    }
}
