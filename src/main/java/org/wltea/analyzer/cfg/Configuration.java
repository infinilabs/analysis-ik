/**
 *
 */
package org.wltea.analyzer.cfg;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;
import org.wltea.analyzer.dic.Dictionary;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Configuration {

    private Environment environment;
    private Settings settings;

    //是否启用智能分词
    private boolean useSmart;

    //是否启用远程词典加载
    private boolean enableRemoteDict = false;

    //是否启用小写处理
    private boolean enableLowercase = true;

    private final List<String> mainDict = new ArrayList<>();
    private final List<String> stopWordsDict = new ArrayList<>();
    private final List<String> quantifierDict = new ArrayList<>();

    @Inject
    public Configuration(Environment env, Settings settings) {
        this.environment = env;
        this.settings = settings;

        this.useSmart = settings.get("use_smart", "false").equals("true");
        this.enableLowercase = settings.get("enable_lowercase", "true").equals("true");
        this.enableRemoteDict = settings.get("enable_remote_dict", "true").equals("true");

        Dictionary.initial(this);

        String[] extDicMains = settings.getAsArray("ext_dic_main");
        if (extDicMains != null) {
            mainDict.addAll(Arrays.asList(extDicMains));
        }
        String[] extDicStops = settings.getAsArray("ext_dic_stop");
        if (extDicStops != null) {
            stopWordsDict.addAll(Arrays.asList(extDicStops));
        }
        String[] quantifiers = settings.getAsArray("ext_dic_quantifier");
        if (quantifiers != null) {
            quantifierDict.addAll(Arrays.asList(quantifiers));
        }

    }

    public Path getConfigInPluginDir() {
        return PathUtils
                .get(new File(AnalysisIkPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath())
                        .getParent(), "config")
                .toAbsolutePath();
    }

    public boolean isUseSmart() {
        return useSmart;
    }

    public Configuration setUseSmart(boolean useSmart) {
        this.useSmart = useSmart;
        return this;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Settings getSettings() {
        return settings;
    }

    public boolean isEnableRemoteDict() {
        return enableRemoteDict;
    }

    public boolean isEnableLowercase() {
        return enableLowercase;
    }

    public List<String> getMainDict() {
        return mainDict;
    }

    public List<String> getStopWordsDict() {
        return stopWordsDict;
    }

    public List<String> getQuantifierDict() {
        return quantifierDict;
    }
}
