package org.wltea.analyzer.fcp;

import org.wltea.analyzer.cfg.Configuration;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @ClassName Configuration4Test
 * @Description:
 */
public class Configuration4Test extends Configuration {
    @Override
    public Path getConfDir() {
        return Paths.get("../", "config");
    }

    @Override
    public Path getConfigInPluginDir() {
        return Paths.get("../", "config");
    }

    @Override
    public Path getPath(String first, String... more) {
        return Paths.get(first, more);
    }
}
