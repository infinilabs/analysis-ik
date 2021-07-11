package org.wltea.analyzer.dictionary;

import org.wltea.analyzer.help.DictionaryHelper;

public class Monitor implements Runnable {

	private String location;

	public Monitor(String location) {
		this.location = location;
	}

	@Override
	public void run() {
		DictionaryHelper.reloadRemoteDictionary(location);
	}
}
