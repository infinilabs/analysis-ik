package org.wltea.analyzer.configuration;

import org.wltea.analyzer.help.StringHelper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * IkConfigurationProperties
 *
 * @author Qicz
 * @since 2021/7/9 10:34
 */
public class ConfigurationProperties {

	/**
	 * 扩展词库
	 */
	private List<String> extDictionaries;

	/**
	 * 扩展stop word库
	 */
	private List<String> extStopWordsDictionaries;

	/**
	 * 远程扩展词库
	 */
	private List<String> remoteExtDictionaries;

	/**
	 * 远程stop word库
	 */
	private List<String> remoteStopWordsDictionaries;
	
	public List<String> getExtDictionaries() {
		return extDictionaries = StringHelper.filterBlank(extDictionaries);
	}

	public void setExtDictionaries(List<String> extDictionaries) {
		this.extDictionaries = StringHelper.filterBlank(extDictionaries);
	}

	public List<String> getExtStopWordsDictionaries() {
		return extStopWordsDictionaries = StringHelper.filterBlank(extStopWordsDictionaries);
	}

	public void setExtStopWordsDictionaries(List<String> extStopWordsDictionaries) {
		this.extStopWordsDictionaries = StringHelper.filterBlank(extStopWordsDictionaries);
	}

	public List<String> getRemoteExtDictionaries() {
		return remoteExtDictionaries = StringHelper.filterBlank(remoteExtDictionaries);
	}

	public void setRemoteExtDictionaries(List<String> remoteExtDictionaries) {
		this.remoteExtDictionaries = StringHelper.filterBlank(remoteExtDictionaries);
	}

	public List<String> getRemoteStopWordsDictionaries() {
		return remoteStopWordsDictionaries = StringHelper.filterBlank(remoteStopWordsDictionaries);
	}

	public void setRemoteStopWordsDictionaries(List<String> remoteStopWordsDictionaries) {
		this.remoteStopWordsDictionaries = StringHelper.filterBlank(remoteStopWordsDictionaries);
	}

	@Override
	public String toString() {
		return "ConfigurationProperties{" +
				"extDictionaries=" + extDictionaries +
				", extStopWordsDictionaries=" + extStopWordsDictionaries +
				", remoteExtDictionaries=" + remoteExtDictionaries +
				", remoteStopWordsDictionaries=" + remoteStopWordsDictionaries +
				'}';
	}
}
