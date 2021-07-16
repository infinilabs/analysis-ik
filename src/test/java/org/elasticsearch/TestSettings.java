package org.elasticsearch;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.junit.Test;
import org.openingo.redip.configuration.RedipConfigurationProperties;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.remote.RemoteDictionary;
import org.wltea.analyzer.configuration.Configuration;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * TestSettings
 *
 * @author Qicz
 * @since 2021/7/9 10:02
 */
public class TestSettings {

	@Test
	public void readYml() {
		try {
		URL resource = Thread.currentThread().getContextClassLoader().getResource("ikanalyzer.yml");
		File file = new File(resource.toURI());
		Path path = file.toPath();
		final Settings customSettings;

			customSettings = Settings.builder().loadFromPath(path).build();
			assert customSettings != null;
			System.out.println(customSettings);
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to load settings", e);
		}
	}

	@Test
	public void loadYml() {
		Yaml yaml = new Yaml(new CustomClassLoaderConstructor(RedipConfigurationProperties.class, TestSettings.class.getClassLoader()));
		InputStream resourceAsStream = TestSettings.class.getClassLoader().getResourceAsStream("ikanalyzer.yml");
		RedipConfigurationProperties map = yaml.loadAs(resourceAsStream, RedipConfigurationProperties.class);
		System.out.println(map);
	}

	@Test
	public void load() throws Exception {
		String path = System.getProperty("user.dir");
		Environment environment = new Environment(Settings.builder().put("path.home", path).build(), null);
		Settings settings = Settings.builder()
				.put("use_smart", false)
				.put("enable_lowercase", false)
				.put("enable_remote_dict", true)
				.put("domain", "ik-domain")
				.build();
		new Configuration(environment, settings).setUseSmart(false);

		int idx = 0;
		while (true) {
			TimeUnit.SECONDS.sleep(3);

			RemoteDictionary.addWord(RemoteDictionaryEtymology.REDIS,
					DictionaryType.MAIN_WORDS, "ik-domain", "新词来了"+idx, "有一个新词来了"+idx);


			RemoteDictionary.addWord(RemoteDictionaryEtymology.REDIS,
					DictionaryType.STOP_WORDS, "ik-domain", "新词stop来了"+idx, "有一个新词stop来了"+idx);
			idx++;
		}
	}
}
