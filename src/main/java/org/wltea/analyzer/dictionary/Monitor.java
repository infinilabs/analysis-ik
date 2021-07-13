package org.wltea.analyzer.dictionary;

import org.wltea.analyzer.help.DictionaryHelper;

import java.net.URI;

public class Monitor implements Runnable {

	private final Dictionary dictionary;
	private final DictionaryType dictionaryType;
	private final URI domainUri;

	public Monitor(Dictionary dictionary, DictionaryType dictionaryType, URI domainUri) {
		this.dictionary = dictionary;
		this.dictionaryType = dictionaryType;
		this.domainUri = domainUri;
	}

	@Override
	public void run() {
		DictionaryHelper.reloadRemoteDictionary(this.dictionary, this.dictionaryType, this.domainUri);
	}
}
