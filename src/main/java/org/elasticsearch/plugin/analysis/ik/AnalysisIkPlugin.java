package org.elasticsearch.plugin.analysis.ik;

import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.AnalyzerProvider;
import org.elasticsearch.index.analysis.IkAnalyzerProvider;
import org.elasticsearch.index.analysis.IkTokenizerFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;
import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AnalysisIkPlugin extends Plugin implements AnalysisPlugin {

    public static String PLUGIN_NAME = "analysis-ik";

    private final static String FILE_NAME = "IKAnalyzer.yml";

    private final Path configPath;

    private static final Logger logger = ESPluginLoggerFactory.getLogger(AnalysisIkPlugin.class.getName());

    private final static String EXT_DICT = "ext_dict";
    private final static String REMOTE_EXT_DICT = "remote_ext_dict";
    private final static String EXT_STOP = "ext_stop_word";
    private final static String REMOTE_EXT_STOP = "remote_ext_stop_word";

    public AnalysisIkPlugin(Settings settings, Path configPath) {
        this.configPath = configPath;
    }

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> getTokenizers() {
        Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> extra = new HashMap<>();


        extra.put("ik_smart", IkTokenizerFactory::getIkSmartTokenizerFactory);
        extra.put("ik_max_word", IkTokenizerFactory::getIkTokenizerFactory);

        return extra;
    }

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> extra = new HashMap<>();

        extra.put("ik_smart", IkAnalyzerProvider::getIkSmartAnalyzerProvider);
        extra.put("ik_max_word", IkAnalyzerProvider::getIkAnalyzerProvider);

        return extra;
    }

    @Override
    public Settings additionalSettings() {
        Path configFile = this.configPath.resolve(PLUGIN_NAME).resolve(FILE_NAME);
        try {
            return Settings.builder().loadFromPath(configFile).build();
        } catch (IOException e) {
            logger.error("ik-analyzer failed to load settings", e);
        }
        return super.additionalSettings();
    }

    @Override
    public List<Setting<?>> getSettings() {
        String[] dictionaries = { EXT_DICT, EXT_STOP, REMOTE_EXT_DICT, REMOTE_EXT_STOP };
        List<Setting<?>> settings = new ArrayList<Setting<?>>();
        for (String dictionary : dictionaries) {
            String[] keyInfo = { PLUGIN_NAME.replace("-", "_"), "dictionary", dictionary };
            String key = String.join(".", keyInfo);
            Setting<String> setting = Setting.simpleString(key, "", Setting.Property.NodeScope);
            settings.add(setting);
        }
        return settings;
    }
}
