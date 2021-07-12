package org.wltea.analyzer.dictionary;

import org.wltea.analyzer.help.DictionaryHelper;

public class Monitor implements Runnable {

	private final Dictionary dictionary;
	private final DictionaryType dictionaryType;
	private final String location;

	public Monitor(Dictionary dictionary, DictionaryType dictionaryType, String location) {
		this.dictionary = dictionary;
		this.dictionaryType = dictionaryType;
		this.location = location;
	}

	@Override
	public void run() {
		DictionaryHelper.reloadRemoteDictionary(this.dictionary, this.dictionaryType, this.location);
	}
}
