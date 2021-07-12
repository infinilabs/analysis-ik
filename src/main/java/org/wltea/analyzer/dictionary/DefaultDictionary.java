package org.wltea.analyzer.dictionary;

import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.configuration.ConfigurationProperties;
import org.wltea.analyzer.help.DictionaryHelper;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * DefaultDictionary
 *
 * @author Qicz
 * @since 2021/7/12 23:34
 */
public class DefaultDictionary {

	private static final Logger logger = ESPluginLoggerFactory.getLogger(DefaultDictionary.class.getName());

	private static final String PATH_DIC_MAIN = "main.dic";
	private static final String PATH_DIC_QUANTIFIER = "quantifier.dic";
	private static final String PATH_DIC_STOP = "stopword.dic";

	private DictSegment mainDictionary;
	private DictSegment quantifierDictionary;
	private DictSegment stopWordsDictionary;

	private static DefaultDictionary defaultDictionary;

	public static synchronized DefaultDictionary initial(ConfigurationProperties properties) {
		if (Objects.isNull(defaultDictionary)) {
			synchronized (DefaultDictionary.class) {
				if (Objects.isNull(defaultDictionary)) {
					defaultDictionary = new DefaultDictionary(properties);
				}
			}
		}
		return defaultDictionary;
	}

	private DefaultDictionary(ConfigurationProperties properties) {
		this.loadMainDict(properties);
		this.loadQuantifierDict();
		this.loadStopWordDict(properties);
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray) {
		return this.mainDictionary.match(charArray);
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray, int begin, int length) {
		return this.mainDictionary.match(charArray, begin, length);
	}

	/**
	 * 检索匹配量词词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
		return this.quantifierDictionary.match(charArray, begin, length);
	}

	/**
	 * 判断是否是停止词
	 *
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray, int begin, int length) {
		return this.stopWordsDictionary.match(charArray, begin, length).isMatch();
	}

	/**
	 * 加载主词典及扩展词典
	 */
	private void loadMainDict(ConfigurationProperties properties) {
		// 建立一个主词典实例
		this.mainDictionary = new DictSegment((char) 0);

		// 读取主词典文件
		Path file = Configuration.getBaseOnDictRoot(DefaultDictionary.PATH_DIC_MAIN);
		this.mainDictionary.fillSegment(file, "Main DictFile");
		// 加载扩展词典
		List<String> mainExtDictFiles = properties.getMainExtDictFiles();
		this.loadLocalExtDict(this.mainDictionary, mainExtDictFiles, "Main Extra DictFile");
	}

	/**
	 * 加载用户扩展的停止词词典
	 */
	private void loadStopWordDict(ConfigurationProperties properties) {
		// 建立主词典实例
		this.stopWordsDictionary = new DictSegment((char) 0);

		// 读取主词典文件
		Path file = Configuration.getBaseOnDictRoot(DefaultDictionary.PATH_DIC_STOP);
		this.stopWordsDictionary.fillSegment(file, "Main Stopwords");

		// 加载扩展停止词典
		List<String> extStopDictFiles = properties.getExtStopDictFiles();
		this.loadLocalExtDict(this.stopWordsDictionary, extStopDictFiles, "Extra Stopwords");
	}

	private void loadLocalExtDict(DictSegment dictSegment, List<String> extDictFiles, String name) {
		// 加载扩展词典配置
		extDictFiles = DictionaryHelper.walkFiles(extDictFiles);
		extDictFiles.forEach(extDictName -> {
			// 读取扩展词典文件
			logger.info("[Local DictFile Loading] " + extDictName);
			Path file = Configuration.getPath(extDictName);
			dictSegment.fillSegment(file, name);
		});
	}

	/**
	 * 加载量词词典
	 */
	private void loadQuantifierDict() {
		// 建立一个量词典实例
		this.quantifierDictionary = new DictSegment((char) 0);
		// 读取量词词典文件
		Path file = Configuration.getBaseOnDictRoot(DefaultDictionary.PATH_DIC_QUANTIFIER);
		this.quantifierDictionary.fillSegment(file,  "Quantifier");
	}
}
