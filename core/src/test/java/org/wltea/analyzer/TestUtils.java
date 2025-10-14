package org.wltea.analyzer;

import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class TestUtils {

    public static Configuration createFakeConfigurationSub(boolean useSmart) {
        FakeConfigurationSub configurationSub = new FakeConfigurationSub(useSmart);
        Dictionary.initial(configurationSub);
        return configurationSub;
    }

    /**
     * ES插件需要指向ES的配置目录，这里使用当前项目的config目录作为配置目录，避免依赖计算机上安装ES
     */
    static class FakeConfigurationSub extends Configuration
    {
        public FakeConfigurationSub(boolean useSmart) {
            this.useSmart = useSmart;
        }

        @Override
        public Path getConfDir() {
            return getConfigDir();
        }

        @Override
        public Path getConfigInPluginDir() {
            return getConfigDir();
        }

        @Override
        public Path getPath(String first, String... more) {
            return FileSystems.getDefault().getPath(first, more);
        }

        private static Path getConfigDir()
        {
            String projectRoot = new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath();
            return new File(projectRoot, "config").toPath();
        }
    }
}
