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
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.help.ESPluginLoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

public class Configuration {
	
	private static final Logger logger = ESPluginLoggerFactory.getLogger(Configuration.class.getName());
	
	private Environment environment;
	private Settings settings;

	//是否启用智能分词
	private boolean useSmart;

	//是否启用远程词典加载
	private boolean enableRemoteDict = false;

	//是否启用小写处理
	private boolean enableLowercase = true;
	
	private final static String IKANALYZER_YML = "ikanalyzer.yml";
	
	private ConfigurationProperties properties;
	private Path configurationDirectory;
	private String dictRoot;
	private boolean loaded = false;

	@Inject
	public Configuration(Environment env, Settings settings) {
		if (!AnalysisIkPlugin.configurationLoaded) {
			this.environment = env;
			this.settings = settings;

			this.useSmart = "true".equals(settings.get("use_smart", "false"));
			this.enableLowercase = "true".equals(settings.get("enable_lowercase", "true"));
			this.enableRemoteDict = "true".equals(settings.get("enable_remote_dict", "true"));

			this.parserConfigurationProperties(env);
			Dictionary.initial(this);
			AnalysisIkPlugin.configurationLoaded = true;
		}
	}
	
	private void parserConfigurationProperties(Environment env) {
		this.configurationDirectory = env.configFile().resolve(AnalysisIkPlugin.PLUGIN_NAME);
		this.dictRoot = this.configurationDirectory.toAbsolutePath().toString();
		Path configFile = this.configurationDirectory.resolve(IKANALYZER_YML);
		InputStream input = null;
		try {
			logger.info("try load config from {}", configFile);
			input = new FileInputStream(configFile.toFile());
		} catch (FileNotFoundException e) {
			this.configurationDirectory = this.getConfigInPluginDir();
			configFile = this.configurationDirectory.resolve(IKANALYZER_YML);
			try {
				logger.info("try load config from {}", configFile);
				input = new FileInputStream(configFile.toFile());
			} catch (FileNotFoundException ex) {
				// We should report origin exception
				logger.error("ik-analyzer", e);
			}
		}
		logger.info("thread === {}", Thread.currentThread());
		if (input != null) {
			SecurityManager sm = System.getSecurityManager();
			if (sm != null) {
				// unprivileged code such as scripts do not have SpecialPermission
				sm.checkPermission(new SpecialPermission());
			}
			Yaml yaml = new Yaml(new CustomClassLoaderConstructor(Configuration.class.getClassLoader()));
			// Yaml yaml = AccessController.doPrivileged ((PrivilegedAction<Yaml>) () -> new Yaml(new CustomClassLoaderConstructor(Configuration.class.getClassLoader())));
			this.properties = yaml.loadAs(input, ConfigurationProperties.class);
		}
	}

	public Path getConfigInPluginDir() {
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

	public Environment getEnvironment() {
		return environment;
	}

	public Settings getSettings() {
		return settings;
	}

	public boolean isEnableRemoteDict() {
		return enableRemoteDict;
	}

	public boolean isEnableLowercase() {
		return enableLowercase;
	}

	public ConfigurationProperties getProperties() {
		return properties;
	}

	public Path getPathBaseOnDictRoot(String name) {
		return this.get(this.dictRoot, name);
	}

	public Path get(String first, String... more) {
		return PathUtils.get(first, more);
	}
}
