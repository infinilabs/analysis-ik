package org.elasticsearch;

import org.elasticsearch.common.settings.Settings;
import org.junit.Test;
import org.wltea.analyzer.configuration.ConfigurationProperties;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

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
		Yaml yaml = new Yaml(new CustomClassLoaderConstructor(ConfigurationProperties.class, TestSettings.class.getClassLoader()));
		InputStream resourceAsStream = TestSettings.class.getClassLoader().getResourceAsStream("ikanalyzer.yml");
		ConfigurationProperties map = yaml.loadAs(resourceAsStream, ConfigurationProperties.class);
		System.out.println(map);
	}
}
