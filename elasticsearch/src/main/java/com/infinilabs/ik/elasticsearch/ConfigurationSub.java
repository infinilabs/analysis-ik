/**
 * 
 */
package com.infinilabs.ik.elasticsearch;

import org.elasticsearch.SpecialPermission;
import org.elasticsearch.core.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;

import java.io.File;
import java.nio.file.Path;

public class ConfigurationSub extends Configuration {
	
	private Environment environment;
	
	public ConfigurationSub(Environment env,Settings settings) {
		this.environment = env;
		this.useSmart = settings.get("use_smart", "false").equals("true");
		this.enableLowercase = settings.get("enable_lowercase", "true").equals("true");
		this.enableRemoteDict = settings.get("enable_remote_dict", "true").equals("true");

		Dictionary.initial(this);

	}

	@Override
	public Path getConfDir() {
		return this.environment.configFile().resolve(AnalysisIkPlugin.PLUGIN_NAME);
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

	public boolean isEnableRemoteDict() {
		return enableRemoteDict;
	}

	public boolean isEnableLowercase() {
		return enableLowercase;
	}

	public Path getPath(String first, String... more) {
		return PathUtils.get(first, more);
	}
	
	public void check(){
		SpecialPermission.check();
	}
}
