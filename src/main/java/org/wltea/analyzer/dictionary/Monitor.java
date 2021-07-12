package org.wltea.analyzer.dictionary;

import org.wltea.analyzer.help.DictionaryHelper;

public class Monitor implements Runnable {

	private final DictionaryType dictionaryType;
	private final String location;

	public Monitor(DictionaryType dictionaryType, String location) {
		this.dictionaryType = dictionaryType;
		this.location = location;
	}

	@Override
	public void run() {
		DictionaryHelper.reloadRemoteDictionary(this.dictionaryType, this.location);
	}
}
