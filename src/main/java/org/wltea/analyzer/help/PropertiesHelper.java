package org.wltea.analyzer.help;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;
import org.wltea.analyzer.cfg.Configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * PropertiesHelper
 *
 * @author Qicz
 * @since 2021/7/8 14:09
 */
public class PropertiesHelper {

	private static final Logger logger = ESPluginLoggerFactory.getLogger(PropertiesHelper.class.getName());

	private final static String FILE_NAME = "IKAnalyzer.cfg.xml";

	private Properties properties;
	private Path confDir;

	public PropertiesHelper(Configuration configuration) {
		this.confDir = configuration.getEnvironment().configFile().resolve(AnalysisIkPlugin.PLUGIN_NAME);
		Path configFile = confDir.resolve(FILE_NAME);
		InputStream input = null;
		try {
			logger.info("try load config from {}", configFile);
			input = new FileInputStream(configFile.toFile());
		} catch (FileNotFoundException e) {
			confDir = configuration.getConfigInPluginDir();
			configFile = confDir.resolve(FILE_NAME);
			try {
				logger.info("try load config from {}", configFile);
				input = new FileInputStream(configFile.toFile());
			} catch (FileNotFoundException ex) {
				// We should report origin exception
				logger.error("ik-analyzer", e);
			}
		}
		if (input != null) {
			try {
				properties.loadFromXML(input);
			} catch (IOException e) {
				logger.error("ik-analyzer", e);
			}
		}
	}

	public String getProperty(String key) {
		if (properties != null) {
			return properties.getProperty(key);
		}
		return null;
	}

	public String getDictRoot() {
		return confDir.toAbsolutePath().toString();
	}

	public Path getPathBaseOnDictRoot(String name) {
		return this.get(this.getDictRoot(), name);
	}

	public Path get(String first, String... more) {
		return PathUtils.get(first, more);
	}
}
