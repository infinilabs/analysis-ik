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
package org.wltea.analyzer.dic;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.SpecialPermission;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.help.ESPluginLoggerFactory;
import org.wltea.analyzer.help.PropertiesHelper;
import org.wltea.analyzer.help.RemoteDictDownloader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
	private static final String PATH_DIC_SURNAME = "surname.dic";
	private static final String PATH_DIC_QUANTIFIER = "quantifier.dic";
	private static final String PATH_DIC_SUFFIX = "suffix.dic";
	private static final String PATH_DIC_PREP = "preposition.dic";
	private static final String PATH_DIC_STOP = "stopword.dic";
	private final static String EXT_DICT = "ext_dict";
	private final static String REMOTE_EXT_DICT = "remote_ext_dict";
	private final static String EXT_STOP = "ext_stopwords";
	private final static String REMOTE_EXT_STOP = "remote_ext_stopwords";
	/*
	 * 词典单子实例
	 */
	private static Dictionary singleton;
	private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
	private DictSegment mainDictionary;
	private DictSegment quantifierDictionary;
	private DictSegment stopWordsDictionary;
	/**
	 * 配置对象
	 */
	private Configuration configuration;
	private PropertiesHelper propertiesHelper;

	private Dictionary(Configuration configuration) {
		this.configuration = configuration;
		this.propertiesHelper = new PropertiesHelper(configuration);
	}

	/**
	 * 词典初始化 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
	 * 只有当Dictionary类被实际调用时，才会开始载入词典， 这将延长首次分词操作的时间 该方法提供了一个在应用加载阶段就初始化字典的手段
	 */
	public static synchronized void initial(Configuration cfg) {
		if (singleton == null) {
			synchronized (Dictionary.class) {
				if (singleton == null) {

					singleton = new Dictionary(cfg);
					singleton.loadMainDict();
					singleton.loadSurnameDict();
					singleton.loadQuantifierDict();
					singleton.loadSuffixDict();
					singleton.loadPrepDict();
					singleton.loadStopWordDict();

					if (cfg.isEnableRemoteDict()) {
						// 建立监控线程
						for (String location : singleton.getExtDictionaries(REMOTE_EXT_DICT, false)) {
							// 10 秒是初始延迟可以修改的 60是间隔时间 单位秒
							pool.scheduleAtFixedRate(new Monitor(location), 10, 60, TimeUnit.SECONDS);
						}
						for (String location : singleton.getExtDictionaries(REMOTE_EXT_STOP, false)) {
							pool.scheduleAtFixedRate(new Monitor(location), 10, 60, TimeUnit.SECONDS);
						}
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
	public static Dictionary getSingleton() {
		if (singleton == null) {
			throw new IllegalStateException("ik dict has not been initialized yet, please call initial method first.");
		}
		return singleton;
	}

	private static List<String> getRemoteWords(String location) {
		SpecialPermission.check();
		return AccessController.doPrivileged((PrivilegedAction<List<String>>) () -> RemoteDictDownloader.getRemoteWordsUnprivileged(location));
	}

	private List<String> getExtDictionaries(String dict, boolean walk) {
		List<String> extDictFiles = new ArrayList<>(2);
		String extDictCfg = this.propertiesHelper.getProperty(dict);
		if (Objects.isNull(extDictCfg)) {
			return extDictFiles;
		}
		String[] filePaths = extDictCfg.split(";");
		for (String filePath : filePaths) {
			if (filePath != null && !"".equals(filePath.trim())) {
				if (walk) {
					Path file = this.propertiesHelper.getPathBaseOnDictRoot(filePath.trim());
					walkFileTree(extDictFiles, file);
				} else {
					extDictFiles.add(filePath);
				}
			}
		}
		return extDictFiles;
	}

	/**
	 * 批量加载新词条
	 *
	 * @param words Collection<String>词条列表
	 */
	public void addWords(Collection<String> words) {
		if (words != null) {
			for (String word : words) {
				if (word != null) {
					// 批量加载词条到主内存词典中
					singleton.mainDictionary.fillSegment(word.trim().toCharArray());
				}
			}
		}
	}

	/**
	 * 批量移除（屏蔽）词条
	 */
	public void disableWords(Collection<String> words) {
		if (words != null) {
			for (String word : words) {
				if (word != null) {
					// 批量屏蔽词条
					singleton.mainDictionary.disableSegment(word.trim().toCharArray());
				}
			}
		}
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray) {
		return singleton.mainDictionary.match(charArray);
	}

	/**
	 * 检索匹配主词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray, int begin, int length) {
		return singleton.mainDictionary.match(charArray, begin, length);
	}

	/**
	 * 检索匹配量词词典
	 *
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
		return singleton.quantifierDictionary.match(charArray, begin, length);
	}

	/**
	 * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
	 *
	 * @return Hit
	 */
	public Hit matchWithHit(char[] charArray, int currentIndex, Hit matchedHit) {
		DictSegment ds = matchedHit.getMatchedDictSegment();
		return ds.match(charArray, currentIndex, 1, matchedHit);
	}

	/**
	 * 判断是否是停止词
	 *
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray, int begin, int length) {
		return singleton.stopWordsDictionary.match(charArray, begin, length).isMatch();
	}

	/**
	 * 加载主词典及扩展词典
	 */
	private void loadMainDict() {
		// 建立一个主词典实例
		mainDictionary = new DictSegment((char) 0);

		// 读取主词典文件
		Path file = this.propertiesHelper.getPathBaseOnDictRoot(Dictionary.PATH_DIC_MAIN);
		loadDictFile(mainDictionary, file, false, "Main Dict");
		// 加载扩展词典
		this.loadExtDict();
		// 加载远程自定义词库
		this.loadRemoteExtDict(mainDictionary, REMOTE_EXT_DICT);
	}

	/**
	 * 加载用户配置的扩展词典到主词库表
	 */
	private void loadExtDict() {
		// 加载扩展词典配置
		List<String> extDictFiles = this.getExtDictionaries(EXT_DICT, true);
		for (String extDictName : extDictFiles) {
			// 读取扩展词典文件
			logger.info("[Dict Loading] " + extDictName);
			Path file = this.propertiesHelper.get(extDictName);
			loadDictFile(mainDictionary, file, false, "Extra Dict");
		}
	}

	/**
	 * 加载用户扩展的停止词词典
	 */
	private void loadStopWordDict() {
		// 建立主词典实例
		stopWordsDictionary = new DictSegment((char) 0);

		// 读取主词典文件
		Path file = this.propertiesHelper.getPathBaseOnDictRoot(Dictionary.PATH_DIC_STOP);
		loadDictFile(stopWordsDictionary, file, false, "Main Stopwords");

		// 加载扩展停止词典
		List<String> extStopWordDictFiles = this.getExtDictionaries(EXT_STOP, true);
		for (String extStopWordDictName : extStopWordDictFiles) {
			logger.info("[Dict Loading] " + extStopWordDictName);

			// 读取扩展词典文件
			file = this.propertiesHelper.get(extStopWordDictName);
			loadDictFile(stopWordsDictionary, file, false, "Extra Stopwords");
		}

		// 加载远程停用词典
		this.loadRemoteExtDict(stopWordsDictionary, REMOTE_EXT_STOP);
	}

	private void loadRemoteExtDict(DictSegment dictSegment, String dict) {
		List<String> remoteExtStopWordDictFiles = this.getExtDictionaries(dict, false);
		for (String location : remoteExtStopWordDictFiles) {
			logger.info("[Dict Loading] " + location);
			List<String> lists = getRemoteWords(location);
			// 如果找不到扩展的字典，则忽略
			if (lists == null) {
				logger.error("[Dict Loading] " + location + " load failed");
				continue;
			}
			for (String theWord : lists) {
				if (theWord != null && !"".equals(theWord.trim())) {
					// 加载远程词典数据到主内存中
					logger.info(theWord);
					dictSegment.fillSegment(theWord.trim().toLowerCase().toCharArray());
				}
			}
		}
	}

	/**
	 * 加载量词词典
	 */
	private void loadQuantifierDict() {
		// 建立一个量词典实例
		quantifierDictionary = new DictSegment((char) 0);
		// 读取量词词典文件
		Path file = this.propertiesHelper.getPathBaseOnDictRoot(Dictionary.PATH_DIC_QUANTIFIER);
		loadDictFile(quantifierDictionary, file, false, "Quantifier");
	}

	private void loadSurnameDict() {
		DictSegment surnameDict = new DictSegment((char) 0);
		Path file = this.propertiesHelper.getPathBaseOnDictRoot(Dictionary.PATH_DIC_SURNAME);
		loadDictFile(surnameDict, file, true, "Surname");
	}

	private void loadSuffixDict() {
		DictSegment suffixDict = new DictSegment((char) 0);
		Path file = this.propertiesHelper.getPathBaseOnDictRoot(Dictionary.PATH_DIC_SUFFIX);
		loadDictFile(suffixDict, file, true, "Suffix");
	}

	private void loadPrepDict() {
		DictSegment prepDict = new DictSegment((char) 0);
		Path file = this.propertiesHelper.getPathBaseOnDictRoot(Dictionary.PATH_DIC_PREP);
		loadDictFile(prepDict, file, true, "Preposition");
	}

	void reLoadMainDict() {
		logger.info("start to reload ik dict.");
		// 新开一个实例加载词典，减少加载过程对当前词典使用的影响
		Dictionary tmpDict = new Dictionary(configuration);
		tmpDict.configuration = getSingleton().configuration;
		tmpDict.loadMainDict();
		tmpDict.loadStopWordDict();
		mainDictionary = tmpDict.mainDictionary;
		stopWordsDictionary = tmpDict.stopWordsDictionary;
		logger.info("reload ik dict finished.");
	}

	private void loadDictFile(DictSegment dict, Path file, boolean critical, String name) {
		try (InputStream is = new FileInputStream(file.toFile())) {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(is, StandardCharsets.UTF_8), 512);
			String word = br.readLine();
			if (word != null) {
				if (word.startsWith("\uFEFF")) {
					word = word.substring(1);
				}
				for (; word != null; word = br.readLine()) {
					word = word.trim();
					if (word.isEmpty()) {
						continue;
					}
					dict.fillSegment(word.toCharArray());
				}
			}
		} catch (FileNotFoundException e) {
			logger.error("ik-analyzer: " + name + " not found", e);
			if (critical) {
				throw new RuntimeException("ik-analyzer: " + name + " not found!!!", e);
			}
		} catch (IOException e) {
			logger.error("ik-analyzer: " + name + " loading failed", e);
		}
	}

	private void walkFileTree(List<String> files, Path path) {
		if (Files.isRegularFile(path)) {
			files.add(path.toString());
		} else if (Files.isDirectory(path)) {
			try {
				Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						files.add(file.toString());
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException e) {
						logger.error("[Ext Loading] listing files", e);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				logger.error("[Ext Loading] listing files", e);
			}
		} else {
			logger.warn("[Ext Loading] file not found: " + path);
		}
	}
}
