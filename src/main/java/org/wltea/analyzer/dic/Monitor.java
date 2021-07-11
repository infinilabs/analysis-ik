package org.wltea.analyzer.dic;

import org.wltea.analyzer.help.RemoteDictHelper;

public class Monitor implements Runnable {

	private String location;

	public Monitor(String location) {
		this.location = location;
	}

	@Override
	public void run() {
		RemoteDictHelper.reloadRemoteDictionary(location);
	}
}
