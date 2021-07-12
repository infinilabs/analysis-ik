package org.wltea.analyzer.help;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.SpecialPermission;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.dictionary.Dictionary;
import org.wltea.analyzer.dictionary.DictionaryType;
import org.wltea.analyzer.dictionary.remote.AbstractRemoteDictionary;
import org.wltea.analyzer.dictionary.remote.RemoteDictionary;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * DictionaryHelper
 *
 * @author Qicz
 * @since 2021/7/8 14:25
 */
public final class DictionaryHelper {

	private static final Logger logger = ESPluginLoggerFactory.getLogger(DictionaryHelper.class.getName());

	public static List<String> walkFiles(List<String> files) {
		List<String> extDictFiles = new ArrayList<>(files.size());
		files.forEach(filePath -> {
			Path path = Configuration.getBaseOnDictRoot(filePath);
			if (Files.isRegularFile(path)) {
				extDictFiles.add(path.toString());
			} else if (Files.isDirectory(path)) {
				try {
					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
							extDictFiles.add(file.toString());
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException e) {
							logger.error("[Ext Loading] listing files", e);
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					logger.error("[Ext Loading] listing files", e);
				}
			} else {
				logger.warn("[Ext Loading] file not found: " + path);
			}
		});
		return StringHelper.filterBlank(extDictFiles);
	}

	public static Set<String> getRemoteWords(Dictionary dictionary,
											 DictionaryType dictionaryType,
											 String domain) {
		// TODO domain find remote dictionary
		URI uri = toUri(domain);
		AbstractRemoteDictionary remoteDictionary = RemoteDictionary.getRemoteDictionary(uri);
		Set<String> remoteWords = Collections.emptySet();
		if (Objects.isNull(remoteDictionary)) {
			return remoteWords;
		}
		SpecialPermission.check();
		remoteWords = AccessController.doPrivileged((PrivilegedAction<Set<String>>) () -> remoteDictionary.getRemoteWords(dictionary, dictionaryType, uri));
		return StringHelper.filterBlank(remoteWords);
	}

	public static void reloadRemoteDictionary(Dictionary dictionary,
											  DictionaryType dictionaryType,
											  String domain) {
		URI uri = toUri(domain);
		AbstractRemoteDictionary remoteDictionary = RemoteDictionary.getRemoteDictionary(uri);
		if (Objects.isNull(remoteDictionary)) {
			return;
		}
		SpecialPermission.check();
		AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
			remoteDictionary.reloadRemoteDictionary(dictionary, dictionaryType, uri);
			return null;
		});
	}

	private static URI toUri(String location) {
		URI uri;
		try {
			uri = new URI(location);
			logger.info("schema {} authority {}", uri.getScheme(), uri.getAuthority());
		} catch (URISyntaxException e) {
			logger.error("parser location to uri error {} ", e.getLocalizedMessage());
			throw new IllegalArgumentException(String.format("the location %s is illegal: %s", location, e.getLocalizedMessage()));
		}
		return uri;
	}
}
