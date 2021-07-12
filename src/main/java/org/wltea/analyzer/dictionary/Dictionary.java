/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 */
package org.wltea.analyzer.dictionary;

import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.configuration.ConfigurationProperties;
import org.wltea.analyzer.dictionary.remote.RemoteDictionary;
import org.wltea.analyzer.help.DictionaryHelper;
import org.wltea.analyzer.help.ESPluginLoggerFactory;
import org.wltea.analyzer.help.StringHelper;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * 词典管理类,单子模式
 */
public class Dictionary {

	private static final Logger logger = ESPluginLoggerFactory.getLogger(Dictionary.class.getName());
	private static final String PATH_DIC_MAIN = "main.dic";
	private static final String PATH_DIC_QUANTIFIER = "quantifier.dic";
	private static final String PATH_DIC_STOP = "stopword.dic";
	/*
	 * 词典单子实例
	 */
	private static Dictionary dictionary;
	private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
	private DictSegment mainDictionary;
	private DictSegment quantifierDictionary;
	private DictSegment stopWordsDictionary;
	/**
	 * 配置对象
	 */
	private Configuration configuration;
	private ConfigurationProperties configurationProperties;

	private Dictionary(Configuration configuration) {
		this.configuration = configuration;
		this.configurationProperties = configuration.getProperties();
	}

	/**
	 * 词典初始化 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
	 * 只有当Dictionary类被实际调用时，才会开始载入词典， 这将延长首次分词操作的时间 该方法提供了一个在应用加载阶段就初始化字典的手段
	 */
	public static synchronized void initial(Configuration configuration) {
		if (dictionary == null) {
			synchronized (Dictionary.class) {
				if (dictionary == null) {
					// 远程词典初始化准备
					RemoteDictionary.prepare(configuration);
					dictionary = new Dictionary(configuration);
					dictionary.loadMainDict();
					dictionary.loadQuantifierDict();
					dictionary.loadStopWordDict();

					if (configuration.isEnableRemoteDict()) {
						logger.info("Remote Dictionary enabled!");
						// 建立监控线程
						ConfigurationProperties configurationProperties = dictionary.configurationProperties;
						List<String> mainRemoteExtDictFiles = configurationProperties.getMainRemoteExtDictFiles();
						List<String> remoteStopDictFiles = configurationProperties.getRemoteStopDictFiles();

						Set<String> allRemoteDictFiles = new HashSet<>();
						allRemoteDictFiles.addAll(mainRemoteExtDictFiles);
						allRemoteDictFiles.addAll(remoteStopDictFiles);
						ConfigurationProperties.RemoteDictFile.Refresh remoteRefresh = configurationProperties.getRemoteRefresh();
						allRemoteDictFiles.forEach(location -> {
							DictionaryType dictionaryType = DictionaryType.MAIN_WORDS;
							if (remoteStopDictFiles.contains(location)) {
								dictionaryType = DictionaryType.STOP_WORDS;
							}
							pool.scheduleAtFixedRate(
									new Monitor(dictionaryType, location),
									remoteRefresh.getDelay(),
									remoteRefresh.getPeriod(),
									TimeUnit.SECONDS);
						});
					}
				}
			}
		}
	}

	/**
	 * 获取词典单子实例
	 *
	 * @return Dictionary 单例对象
	 */
	public static Dictionary getDictionary() {
		if (dictionary == null) {
			throw new IllegalStateException("ik dict has not been initialized yet, please call initial method first.");
		}
		return dictionary;
	}

	/**
	 * 批量加载新词条
	 *
	 * @param words Collection<String>词条列表
	 */
	public void addWords(Collection<String> words) {
		this.addWords(words, true);
	}

	/**
	 * 批量移除（屏蔽）词条
	 */
	public void disableWords(Collection<String> words) {
		this.addWords(words, false);
	}

	/**
	 * 批量加载新词条
	 *
	 * @param words Collection<String>词条列表
	 * @param enabled true表明一个完整的词，false表示从词典中屏蔽当前词
	 */
	private void addWords(Collection<String> words, boolean enabled) {
		if (Objects.isNull(words)) {
			return;
		}
		StringHelper.filterBlank(words).forEach(word -> {
			// 批量加载词条到主内存词典中或stop word中
			dictionary.mainDictionary.fillSegment(word.toCharArray(), enabled);
		});
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray) {
		return dictionary.mainDictionary.match(charArray);
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray, int begin, int length) {
		return dictionary.mainDictionary.match(charArray, begin, length);
	}

	/**
	 * 检索匹配量词词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
		return dictionary.quantifierDictionary.match(charArray, begin, length);
	}

	/**
	 * 判断是否是停止词
	 *
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray, int begin, int length) {
		return dictionary.stopWordsDictionary.match(charArray, begin, length).isMatch();
	}

	/**
	 * 加载主词典及扩展词典
	 */
	private void loadMainDict() {
		// 建立一个主词典实例
		this.mainDictionary = new DictSegment((char) 0);

		// 读取主词典文件
		Path file = this.configuration.getBaseOnDictRoot(Dictionary.PATH_DIC_MAIN);
		this.mainDictionary.fillSegment(file, "Main DictFile");
		// 加载扩展词典
		List<String> mainExtDictFiles = this.configurationProperties.getMainExtDictFiles();
		this.loadLocalExtDict(this.mainDictionary, mainExtDictFiles, "Main Extra DictFile");

		// 加载远程自定义词库
		List<String> mainRemoteExtDictFiles = this.configurationProperties.getMainRemoteExtDictFiles();
		this.loadRemoteExtDict(this.mainDictionary, DictionaryType.MAIN_WORDS, mainRemoteExtDictFiles);
	}

	/**
	 * 加载用户扩展的停止词词典
	 */
	private void loadStopWordDict() {
		// 建立主词典实例
		this.stopWordsDictionary = new DictSegment((char) 0);

		// 读取主词典文件
		Path file = this.configuration.getBaseOnDictRoot(Dictionary.PATH_DIC_STOP);
		this.stopWordsDictionary.fillSegment(file, "Main Stopwords");

		// 加载扩展停止词典
		List<String> extStopDictFiles = this.configurationProperties.getExtStopDictFiles();
		this.loadLocalExtDict(this.stopWordsDictionary, extStopDictFiles, "Extra Stopwords");

		// 加载远程停用词典
		List<String> remoteStopDictFiles = this.configurationProperties.getRemoteStopDictFiles();
		this.loadRemoteExtDict(this.stopWordsDictionary, DictionaryType.STOP_WORDS, remoteStopDictFiles);
	}

	private void loadLocalExtDict(DictSegment dictSegment, List<String> extDictFiles, String name) {
		// 加载扩展词典配置
		extDictFiles = DictionaryHelper.walkFiles(extDictFiles, this.configuration);
		extDictFiles.forEach(extDictName -> {
			// 读取扩展词典文件
			logger.info("[Local DictFile Loading] " + extDictName);
			Path file = this.configuration.get(extDictName);
			dictSegment.fillSegment(file, name);
		});
	}

	private void loadRemoteExtDict(DictSegment dictSegment,
								   DictionaryType dictionaryType,
								   List<String> remoteDictFiles) {
		remoteDictFiles.forEach(location -> {
			logger.info("[Remote DictFile Loading] " + location);
			Set<String> remoteWords = DictionaryHelper.getRemoteWords(dictionaryType, location);
			// 如果找不到扩展的字典，则忽略
			if (remoteWords.isEmpty()) {
				logger.error("[Remote DictFile Loading] " + location + " load failed");
				return;
			}
			remoteWords.forEach(word -> {
				// 加载远程词典数据到主内存中
				logger.info("[New Word] {}", word);
				dictSegment.fillSegment(word.toLowerCase().toCharArray());
			});
		});
	}

	/**
	 * 加载量词词典
	 */
	private void loadQuantifierDict() {
		// 建立一个量词典实例
		this.quantifierDictionary = new DictSegment((char) 0);
		// 读取量词词典文件
		Path file = this.configuration.getBaseOnDictRoot(Dictionary.PATH_DIC_QUANTIFIER);
		this.quantifierDictionary.fillSegment(file,  "Quantifier");
	}

	/**
	 * 重新加载词典
	 */
	public synchronized void reload(DictionaryType dictionaryType) {
		logger.info("[Begin to reload] ik {} dictionary.", dictionaryType);
		// 新开一个实例加载词典，减少加载过程对当前词典使用的影响
		Dictionary tmpDict = new Dictionary(configuration);
		tmpDict.configuration = getDictionary().configuration;
		switch (dictionaryType) {
			case MAIN_WORDS: {
				tmpDict.loadMainDict();
				this.mainDictionary = tmpDict.mainDictionary;
			}
				break;
			case STOP_WORDS: {
				tmpDict.loadStopWordDict();
				this.stopWordsDictionary = tmpDict.stopWordsDictionary;
			}
				break;
			default: {
				tmpDict.loadMainDict();
				tmpDict.loadStopWordDict();
				this.mainDictionary = tmpDict.mainDictionary;
				this.stopWordsDictionary = tmpDict.stopWordsDictionary;
			}
		}
		logger.info("Reload ik {} dictionary finished.", dictionaryType);
	}
}
