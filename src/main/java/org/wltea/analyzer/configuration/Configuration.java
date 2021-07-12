/**
 *
 */
package org.wltea.analyzer.configuration;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;
import org.wltea.analyzer.dictionary.DefaultDictionary;
import org.wltea.analyzer.dictionary.Dictionary;
import org.wltea.analyzer.dictionary.remote.RemoteDictionary;
import org.wltea.analyzer.help.ESPluginLoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class Configuration {
	
	private static final Logger logger = ESPluginLoggerFactory.getLogger(Configuration.class.getName());

	//是否启用智能分词
	private boolean useSmart;

	//是否启用远程词典加载
	private boolean enableRemoteDict;

	//是否启用小写处理
	private boolean enableLowercase;

	private final static String IKANALYZER_YML = "ikanalyzer.yml";

	private static Boolean initialed = false;
	private static ConfigurationProperties properties;
	private static String dictRootPath;
	private Dictionary dictionary;

	@Inject
	public Configuration(Environment env, Settings settings) {
		// 从 settings 中获取部分配置
		this.useSmart = "true".equals(settings.get("use_smart", "false"));
		this.enableLowercase = "true".equals(settings.get("enable_lowercase", "true"));
		this.enableRemoteDict = "true".equals(settings.get("enable_remote_dict", "true"));
		String domain = settings.get("domain", "");
		// 配置初始化
		Configuration.initial(env);
		// 初始化默认词库
		DefaultDictionary defaultDictionary = DefaultDictionary.initial(properties);
		this.dictionary = Dictionary.initial(this, defaultDictionary, domain);
	}

	private synchronized static void initial(Environment env) {
		if (Configuration.initialed) {
			logger.info("the properties is initialed");
			return;
		}
		// 加载配置文件
		Path pluginPath = env.configFile().resolve(AnalysisIkPlugin.PLUGIN_NAME);
		Configuration.dictRootPath = pluginPath.toAbsolutePath().toString();
		Path configFile = pluginPath.resolve(IKANALYZER_YML);
		InputStream input = null;
		try {
			logger.info("try load config from {}", configFile);
			input = new FileInputStream(configFile.toFile());
		} catch (FileNotFoundException e) {
			pluginPath = getConfigInPluginDir();
			configFile = pluginPath.resolve(IKANALYZER_YML);
			try {
				logger.info("try load config from {}", configFile);
				input = new FileInputStream(configFile.toFile());
			} catch (FileNotFoundException ex) {
				// We should report origin exception
				logger.error("ik-analyzer", ex);
			}
		}
		if (input != null) {
			InputStream finalInput = input;
			SpecialPermission.check();
			Configuration.properties = AccessController.doPrivileged((PrivilegedAction<ConfigurationProperties>) () -> new Yaml(new CustomClassLoaderConstructor(Configuration.class.getClassLoader())).loadAs(finalInput, ConfigurationProperties.class));
		}

		// 远程词典初始化准备
		RemoteDictionary.initial();
		Configuration.initialed = true;
	}

	public static Path getConfigInPluginDir() {
		return PathUtils
				.get(new File(AnalysisIkPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath())
						.getParent(), "config")
				.toAbsolutePath();
	}

	public boolean isUseSmart() {
		return useSmart;
	}

	public Configuration setUseSmart(boolean useSmart) {
		this.useSmart = useSmart;
		return this;
	}

	public boolean isEnableRemoteDict() {
		return enableRemoteDict;
	}

	public boolean isEnableLowercase() {
		return enableLowercase;
	}

	public Dictionary getDictionary() {
		return dictionary;
	}

	public static ConfigurationProperties getProperties() {
		return properties;
	}

	public static Path getBaseOnDictRoot(String name) {
		return Configuration.getPath(dictRootPath, name);
	}

	public static String getDictRootPath() {
		return dictRootPath;
	}

	public static Path getPath(String first, String... more) {
		return PathUtils.get(first, more);
	}
}
