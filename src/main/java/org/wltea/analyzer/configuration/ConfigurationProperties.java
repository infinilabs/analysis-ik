package org.wltea.analyzer.configuration;

import lombok.Data;
import org.wltea.analyzer.help.StringHelper;

import java.util.Collections;
import java.util.List;

/**
 * ConfigurationProperties
 *
 * @author Qicz
 * @since 2021/7/9 10:34
 */
@Data
public class ConfigurationProperties {

	/**
	 * 扩展词库
	 */
	Dict dict = new Dict();

	public final List<String> getMainExtDictFiles() {
		return StringHelper.filterBlank(dict.local.main);
	}

	public final List<String> getExtStopDictFiles() {
		return StringHelper.filterBlank(dict.local.stop);
	}

	public final Remote.Refresh getRemoteRefresh() {
		return this.dict.remote.getRefresh();
	}

	@Data
	public static class Dict {

		/**
		 * 本地词库文件
		 */
		DictFile local = new DictFile();

		/**
		 * 远程词库文件
		 */
		Remote remote = new Remote();
	}

	@Data
	public static class Remote {

		/**
		 * http 配置
		 */
		Http http;

		/**
		 * mysql 配置
		 */
		MySQL mysql = new MySQL();

		/**
		 * redis 配置
		 */
		Redis redis = new Redis();

		Refresh refresh = new Refresh();

		/**
		 * 默认延迟10s，周期60s
		 */
		@Data
		public static class Refresh {
			Integer delay = 10;
			Integer period = 60;
		}
	}

	/**
	 * 词典文件
	 */
	@Data
	public static class DictFile {

		/**
		 * 主词典文件
		 */
		List<String> main = Collections.emptyList();

		/**
		 * stop词典文件
		 */
		List<String> stop = Collections.emptyList();
	}

	@Data
	public static class Http {
		String base = "http://localhost";
	}

	@Data
	public static class MySQL {

		private String url;
		private String username;
		private String password;
	}

	@Data
	public static class Redis {

		private String host = "localhost";
		private Integer port = 6379;
		private String username;
		private String password;
		private Integer database = 0;
	}
}
