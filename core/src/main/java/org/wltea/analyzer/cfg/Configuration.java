package org.wltea.analyzer.cfg;


import java.nio.file.Path;

public abstract class Configuration {

	//是否启用智能分词
	protected boolean useSmart = false;

	//是否启用远程词典加载
	protected boolean enableRemoteDict = false;

	//是否启用小写处理
	protected boolean enableLowercase = true;

	
	public Configuration() {
	}

	public abstract Path getConfDir();
	
	public abstract Path getConfigInPluginDir();

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

	public Configuration setEnableLowercase(boolean enableLowercase) {
		this.enableLowercase = enableLowercase;
		return this;
	}
	
	public abstract Path getPath(String first, String... more);
	
	public void check(){}
}
