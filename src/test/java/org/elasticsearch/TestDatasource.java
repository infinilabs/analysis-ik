package org.elasticsearch;

import com.zaxxer.hikari.HikariDataSource;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.Test;
import org.wltea.analyzer.configuration.ConfigurationProperties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * TestDatasource
 *
 * @author Qicz
 * @since 2021/7/12 10:36
 */
public class TestDatasource {

	private HikariDataSource initDataSource() {
		HikariDataSource dataSource = new HikariDataSource();
		ConfigurationProperties.MySQL mysql = new ConfigurationProperties.MySQL();
		mysql.setUrl("jdbc:mysql://127.0.0.1/ik-db?useSSL=false&serverTimezone=GMT%2B8");
		mysql.setUsername("root");
		mysql.setPassword("dbadmin");
		dataSource.setJdbcUrl(mysql.getUrl());
		dataSource.setUsername(mysql.getUsername());
		dataSource.setPassword(mysql.getPassword());
		return dataSource;
	}

	@Test
	public void testConnection() throws SQLException {
		HikariDataSource dataSource = this.initDataSource();
		Connection connection = dataSource.getConnection();
		String sql = String.format("SELECT MAX(dict.id) max_id, seq.current_id current_id FROM %s dict, ik_sequence seq", "ik_stop_words");
		sql = String.format("%s WHERE seq.dictionary = ? LIMIT 1", sql) ;
		PreparedStatement statement = connection.prepareStatement(sql);
		statement.setString(1, "ik_stop_words");
		ResultSet resultSet = statement.executeQuery();
		if (!resultSet.next()) {
			return;
		}
		long maxId = resultSet.getLong("max_id");
		long currentId = resultSet.getLong("current_id");
	}

	private RedisClient initRedisClient() {
		RedisURI.Builder builder = RedisURI.builder()
				.withHost("localhost");
		return RedisClient.create(builder.build());
	}

	@Test
	public void testRedis() {
		RedisClient redisClient = initRedisClient();
		RedisCommands<String, String> sync = redisClient.connect().sync();
		List<String> ik = sync.lrange("ik", 0, -1);
		System.out.println(new HashSet<>(ik));
	}
}
