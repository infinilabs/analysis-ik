package org.wltea.analyzer.dictionary;

import org.elasticsearch.SpecialPermission;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.dictionary.remote.RemoteDictionary;

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
		SpecialPermission.check();
		RemoteDictionary.reloadRemoteDictionary(this.dictionary, this.dictionaryType, this.domainUri);
	}
}
