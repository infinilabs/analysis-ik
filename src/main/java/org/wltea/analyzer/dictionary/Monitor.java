package org.wltea.analyzer.dictionary;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.SpecialPermission;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.remote.RemoteDictionary;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class Monitor implements Runnable {
	
	private final Dictionary dictionary;
	private final DictionaryType dictionaryType;
	private final URI domainUri;
	private final Set<String> httpRemoteMainDict;
	private final Set<String> httpRemoteStopDict;

	public Monitor(Dictionary dictionary, DictionaryType dictionaryType, URI domainUri, Set<String> httpRemoteMainDict, Set<String> httpRemoteStopDict) {
		this.dictionary = dictionary;
		this.dictionaryType = dictionaryType;
		this.domainUri = domainUri;
		this.httpRemoteMainDict = httpRemoteMainDict;
		this.httpRemoteStopDict = httpRemoteStopDict;
		log.info("monitor dictionary '{}' type '{}' domainUri '{}'", dictionary, dictionaryType, domainUri);
	}

	@Override
	public void run() {
		SpecialPermission.check();
		if (Objects.isNull(this.httpRemoteMainDict) && Objects.isNull(this.httpRemoteStopDict)) {
			RemoteDictionary.reloadRemoteDictionary(this.dictionary, this.dictionaryType, this.domainUri);
		} else  {
			final String etymology = RemoteDictionaryEtymology.HTTP.getEtymology();
			if (Objects.nonNull(this.httpRemoteMainDict)) {
				for (String location : this.httpRemoteMainDict) {
					RemoteDictionary.reloadRemoteDictionary(this.dictionary, DictionaryType.MAIN_WORDS, URI.create(String.format("%s:%s", etymology, location)));
				}
			}
			if (Objects.nonNull(this.httpRemoteStopDict)) {
				for (String location : this.httpRemoteStopDict) {
					RemoteDictionary.reloadRemoteDictionary(this.dictionary, DictionaryType.STOP_WORDS, URI.create(String.format("%s:%s", etymology, location)));
				}
			}
		}
	}
}
