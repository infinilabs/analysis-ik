package org.wltea.analyzer.configuration;

import lombok.Data;
import org.wltea.analyzer.help.StringHelper;

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
	private Dict dict;

	@Data
	public static class Dict {

		/**
		 * 本地词库文件
		 */
		private DictFile local;

		/**
		 * 远程词库文件
		 */
		private DictFile remote;
	}

	/**
	 * 词典文件
	 */
	@Data
	public static class DictFile {

		/**
		 * 主词典文件
		 */
		private List<String> main;

		/**
		 * stop词典文件
		 */
		private List<String> stop;
	}

	public final List<String> getMainExtDictFiles() {
		return StringHelper.filterBlank(dict.local.main);
	}

	public final List<String> getExtStopDictFiles() {
		return StringHelper.filterBlank(dict.local.stop);
	}

	public final List<String> getMainRemoteExtDictFiles() {
		return StringHelper.filterBlank(dict.remote.main);
	}

	public final List<String> getRemoteStopDictFiles() {
		return StringHelper.filterBlank(dict.remote.stop);
	}
}
