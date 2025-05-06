package com.infinilabs.ik.elasticsearch;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class IkAnalyzerProvider extends AbstractIndexAnalyzerProvider<IKAnalyzer> {
    private final IKAnalyzer analyzer;

    public IkAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings, boolean useSmart) {
        super(name);
        // Get the enable_lowercase setting from analyzer settings, default to true
        boolean enableLowercase = settings.getAsBoolean("enable_lowercase", true);

        Configuration configuration = new ConfigurationSub(env, settings).setUseSmart(useSmart).setEnableLowercase(enableLowercase);
        analyzer = new IKAnalyzer(configuration);
    }

    public static IkAnalyzerProvider getIkSmartAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        return new IkAnalyzerProvider(indexSettings, env, name, settings, true);
    }

    public static IkAnalyzerProvider getIkAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        return new IkAnalyzerProvider(indexSettings, env, name, settings, false);
    }

    @Override
    public IKAnalyzer get() {
        return this.analyzer;
    }
}
