/**
 *
 */
package org.wltea.analyzer.configuration;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;
import org.openingo.jdkits.lang.StrKit;
import org.openingo.jdkits.validate.AssertKit;
import org.openingo.redip.configuration.RedipConfigurationProperties;
import org.openingo.redip.configuration.RemoteConfiguration;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.remote.RemoteDictionary;
import org.wltea.analyzer.dictionary.Dictionary;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

@Slf4j
public class Configuration {
	
	//是否启用智能分词
	private boolean useSmart;

	//是否启用远程词典加载
	private boolean enableRemoteDict;

	//是否启用小写处理
	private boolean enableLowercase;

	private final static String IKANALYZER_YML = "ikanalyzer.yml";
	private final static String IKANALYZER_XML = "IKAnalyzer.cfg.xml";

	private static Boolean initialed = false;
	private static RedipConfigurationProperties properties;
	private static Properties xmlProperties;
	private static boolean usingYml = true;
	private static String dictRootPath;
	private Dictionary dictionary;

	@Inject
	public Configuration(Environment env, Settings settings) {
		// 从 settings 中获取部分配置
		this.useSmart = "true".equals(settings.get("use_smart", "false"));
		this.enableLowercase = "true".equals(settings.get("enable_lowercase", "true"));
		this.enableRemoteDict = "true".equals(settings.get("enable_remote_dict", "true"));
		// 词源
		String etymology = settings.get("etymology", RemoteDictionaryEtymology.DEFAULT.getEtymology());
		RemoteDictionaryEtymology settingEtymology = RemoteDictionaryEtymology.newEtymology(etymology);
		String message = String.format("the etymology '%s' config is invalid, just support 'redis','mysql','http'.", etymology);
		AssertKit.notNull(settingEtymology, message);
		// 领域
		String domain = settings.get("domain", "default-domain");
		log.info("new configuration for domain '{}' etymology '{}'", domain, etymology);
		// 配置初始化
		Configuration.initial(env);
		// 校验配置
		checkConfiguration(settingEtymology);
		Set<String> httpRemoteMainDict = null;
		Set<String> httpRemoteStopDict = null;
		if (!Configuration.usingYml && RemoteDictionaryEtymology.HTTP.equals(settingEtymology)) {
			httpRemoteMainDict = new HashSet<>(getXmlPropertyToList("remote_ext_dict"));
			httpRemoteStopDict = new HashSet<>(getXmlPropertyToList("remote_ext_stopwords"));
		}
		// 构造词源及领域
		URI domainUri = URI.create(String.format("%s://%s", etymology, domain));
		this.dictionary = Dictionary.initial(this.enableRemoteDict, domainUri, httpRemoteMainDict, httpRemoteStopDict);
	}

	private synchronized static void initial(Environment env) {
		if (Configuration.initialed) {
			return;
		}
		boolean ret = true;
		// 加载配置文件
		Path pluginPath = env.configFile().resolve(AnalysisIkPlugin.PLUGIN_NAME);
		Configuration.dictRootPath = pluginPath.toAbsolutePath().toString();
		final List<String> files = Arrays.asList(IKANALYZER_YML, IKANALYZER_XML);
		InputStream input = null;
		for (String file : files) {
			input = loadFile(pluginPath, file);
			if (Objects.nonNull(input)) {
				log.info("using '{}' configuration file.", file);
				ret = file.equals(IKANALYZER_YML);
				break;
			}
		}
		AssertKit.isTrue(Objects.nonNull(input), "load configuration failure.");
		if (ret) {
			InputStream finalInput = input;
			SpecialPermission.check();
			Configuration.properties = AccessController.doPrivileged((PrivilegedAction<RedipConfigurationProperties>) () -> new Yaml(new CustomClassLoaderConstructor(Configuration.class.getClassLoader())).loadAs(finalInput, RedipConfigurationProperties.class));
		} else {
			try {
				final Properties properties = new Properties();
				properties.loadFromXML(input);
				Configuration.xmlProperties = properties;
				Configuration.properties = parserXml();
			} catch (IOException e) {
				log.error("ik-analyzer", e);
			}
		}

		// 远程词典初始化准备
		RemoteDictionary.initial(Configuration.properties);
		Configuration.initialed = true;
		usingYml = ret;
	}

	private void checkConfiguration(RemoteDictionaryEtymology settingEtymology) {
		final RedipConfigurationProperties.Remote remote = Configuration.properties.getRemote();
		String configFile = usingYml ? IKANALYZER_YML : IKANALYZER_XML;
		String message = String.format("the '%s' config not found in '%s'", settingEtymology.getEtymology(), configFile);
		if (RemoteDictionaryEtymology.HTTP.equals(settingEtymology)) {
			AssertKit.isTrue(Objects.nonNull(remote.getHttp()), message);
		}
		if (RemoteDictionaryEtymology.REDIS.equals(settingEtymology)) {
			AssertKit.isTrue(Objects.nonNull(remote.getRedis()), message);
		}
		if (RemoteDictionaryEtymology.MYSQL.equals(settingEtymology)) {
			AssertKit.isTrue(Objects.nonNull(remote.getMysql()), message);
		}
	}

	private static RedipConfigurationProperties parserXml() {
		final RedipConfigurationProperties redipConfigurationProperties = new RedipConfigurationProperties();
		final List<String> localMainExt = getXmlPropertyToList("ext_dict");
		final List<String> localStopExt = getXmlPropertyToList("ext_stopwords");
		final RedipConfigurationProperties.DictFile local = redipConfigurationProperties.getDict().getLocal();
		local.setMain(localMainExt);
		local.setStop(localStopExt);
		final RedipConfigurationProperties.Remote remote = redipConfigurationProperties.getDict().getRemote();
		// redis config
		final Properties xmlProperties = Configuration.xmlProperties;
		final String redisHost = xmlProperties.getProperty("redis.host", "");
		RemoteConfiguration.Redis redis = null;
		if (StrKit.notBlank(redisHost)) {
			final String redisPort = xmlProperties.getProperty("redis.port", "6379");
			final String redisUsername = xmlProperties.getProperty("redis.username");
			final String redisPassword = xmlProperties.getProperty("redis.password");
			final String redisDb = xmlProperties.getProperty("redis.database");
			redis = new RemoteConfiguration.Redis();
			redis.setHost(redisHost);
			redis.setPort(Integer.valueOf(redisPort));
			redis.setUsername(redisUsername);
			redis.setPassword(redisPassword);
			redis.setDatabase(Integer.valueOf(redisDb));
		}
		remote.setRedis(redis);
		// mysql config
		final String mysqlUrl = xmlProperties.getProperty("mysql.url");
		RemoteConfiguration.MySQL mysql = null;
		if (StrKit.notBlank(mysqlUrl)) {
			final String mysqlUsername = xmlProperties.getProperty("mysql.username");
			final String mysqlPassword = xmlProperties.getProperty("mysql.password");
			mysql = new RemoteConfiguration.MySQL();
			mysql.setUrl(mysqlUrl);
			mysql.setUsername(mysqlUsername);
			mysql.setPassword(mysqlPassword);
		}
		remote.setMysql(mysql);
		// refresh config
		RedipConfigurationProperties.Remote.Refresh refresh = new RedipConfigurationProperties.Remote.Refresh();
		refresh.setDelay(Integer.valueOf(xmlProperties.getProperty("refresh.delay", "10")));
		refresh.setPeriod(Integer.valueOf(xmlProperties.getProperty("refresh.period", "60")));
		remote.setRefresh(refresh);

		return redipConfigurationProperties;
	}

	private static List<String> getXmlPropertyToList(String key) {
		if (StrKit.notBlank(key)) {
			final String property = Configuration.xmlProperties.getProperty(key);
			if (Objects.nonNull(property)) {
				final String[] split = property.split(";");
				return Arrays.asList(split);
			}
		}
		return Collections.emptyList();
	}

	private static InputStream loadFile(Path pluginPath, String file) {
		Path configFile = pluginPath.resolve(file);
		InputStream input = null;
		try {
			log.info("try load config from {}", configFile);
			input = new FileInputStream(configFile.toFile());
		} catch (FileNotFoundException e) {
			pluginPath = getConfigInPluginDir();
			configFile = pluginPath.resolve(file);
			try {
				log.info("try load config from {}", configFile);
				input = new FileInputStream(configFile.toFile());
			} catch (FileNotFoundException ex) {
				// We should report origin exception
				log.error("ik-analyzer", ex);
			}
		}
		return input;
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

	public static RedipConfigurationProperties getProperties() {
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
