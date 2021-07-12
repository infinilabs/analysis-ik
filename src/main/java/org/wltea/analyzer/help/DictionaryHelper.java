package org.wltea.analyzer.help;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.SpecialPermission;
import org.wltea.analyzer.configuration.Configuration;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DictionaryHelper
 *
 * @author Qicz
 * @since 2021/7/8 14:25
 */
public final class DictionaryHelper {

	private static final Logger logger = ESPluginLoggerFactory.getLogger(DictionaryHelper.class.getName());

	public static List<String> walkFiles(List<String> files, Configuration configuration) {
		List<String> extDictFiles = new ArrayList<>(files.size());
		files.forEach(filePath -> {
			Path path = configuration.getBaseOnDictRoot(filePath);
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

	public static List<String> getRemoteWords(DictionaryType dictionaryType, String location) {
		URI uri = toUri(location);
		AbstractRemoteDictionary remoteDictionary = RemoteDictionary.getRemoteDictionary(uri);
		List<String> remoteWords = Collections.emptyList();
		if (Objects.isNull(remoteDictionary)) {
			return remoteWords;
		}
		SpecialPermission.check();
		remoteWords = AccessController.doPrivileged((PrivilegedAction<List<String>>) () -> remoteDictionary.getRemoteWords(dictionaryType, uri));
		return StringHelper.filterBlank(remoteWords);
	}

	public static void reloadRemoteDictionary(DictionaryType dictionaryType, String location) {
		URI uri = toUri(location);
		AbstractRemoteDictionary remoteDictionary = RemoteDictionary.getRemoteDictionary(uri);
		if (Objects.isNull(remoteDictionary)) {
			return;
		}
		SpecialPermission.check();
		AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
			remoteDictionary.reloadRemoteDictionary(dictionaryType, uri);
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
