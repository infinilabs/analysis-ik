package org.wltea.analyzer.help;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.SpecialPermission;
import org.wltea.analyzer.dic.remote.IRemoteDictionary;
import org.wltea.analyzer.dic.remote.RemoteDictionary;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * RemoteDictHelper
 *
 * @author Qicz
 * @since 2021/7/8 14:25
 */
public final class RemoteDictHelper {

	private static final Logger logger = ESPluginLoggerFactory.getLogger(RemoteDictHelper.class.getName());

	public static List<String> getRemoteWords(String location) {
		URI uri = toUri(location);
		IRemoteDictionary remoteDictionary = RemoteDictionary.getRemoteDictionary(uri);
		List<String> remoteWords = Collections.emptyList();
		if (Objects.isNull(remoteDictionary)) {
			return remoteWords;
		}
		SpecialPermission.check();
		remoteWords = AccessController.doPrivileged((PrivilegedAction<List<String>>) () -> remoteDictionary.getRemoteWords(uri));
		return StringHelper.filterBlank(remoteWords);
	}

	public static void reloadRemoteDictionary(String location) {
		URI uri = toUri(location);
		IRemoteDictionary remoteDictionary = RemoteDictionary.getRemoteDictionary(uri);
		if (Objects.isNull(remoteDictionary)) {
			return;
		}
		SpecialPermission.check();
		AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
			remoteDictionary.reloadRemoteDictionary(uri);
			return null;
		});
	}

	private static URI toUri(String location) {
		URI uri;
		try {
			uri = new URI(location);
		} catch (URISyntaxException e) {
			logger.error("parser location to uri error {} ", e.getLocalizedMessage());
			throw new IllegalArgumentException(String.format("the location %s is illegal: %s", location, e.getLocalizedMessage()));
		}
		return uri;
	}
}
