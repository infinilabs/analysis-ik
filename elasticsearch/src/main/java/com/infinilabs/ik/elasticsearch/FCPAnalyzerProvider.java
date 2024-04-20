package com.infinilabs.ik.elasticsearch;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.wltea.analyzer.fcp.CombineCharFilter;
import org.wltea.analyzer.fcp.ExtendFilter;
import org.wltea.analyzer.fcp.FCPAnalyzer;


/**
 * fcp analyzer
 *
 */
public class FCPAnalyzerProvider extends AbstractIndexAnalyzerProvider<FCPAnalyzer> {
    private final FCPAnalyzer analyzer;

    /**
     * indexMode 作为重要的参数，
     * @param indexSettings
     * @param env
     * @param name
     * @param settings
     * @param indexMode
     */
    public FCPAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings, boolean indexMode) {
        super(name, settings);
        boolean splitComplete = settings.getAsBoolean("split_complete", FCPAnalyzer.DEFAULT_SPLIT_COMPLETE);
        int maxTokenLength = settings.getAsInt("max_token_length", CombineCharFilter.DEFAULT_MAX_WORD_LEN);
        boolean uselessMapping = settings.getAsBoolean("useless_mapping", ExtendFilter.DEFAULT_USELESS_MAPPING);
        boolean ignoreBlank = settings.getAsBoolean("ignore_blank", ExtendFilter.DEFAULT_IGNORE_BLANK);
        boolean useFirstPos = settings.getAsBoolean("use_first_position", ExtendFilter.DEFAULT_USE_FIRST_POSITION);
        Boolean showOffset = settings.getAsBoolean("show_offset", null);
        analyzer = new FCPAnalyzer(indexMode);
        if (showOffset != null) {
            analyzer.setShowOffset(showOffset);
        }
        analyzer.setSplitComplete(splitComplete);
        analyzer.setUselessMapping(uselessMapping);
        analyzer.setMaxTokenLength(maxTokenLength);
        analyzer.setIgnoreBlank(ignoreBlank);
        analyzer.setUseFirstPos(useFirstPos);
    }

    public static FCPAnalyzerProvider getFCPAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        boolean indexMode = settings.getAsBoolean("index_mode", ExtendFilter.DEFAULT_INDEX_MODE);
        return new FCPAnalyzerProvider(indexSettings, env, name, settings, indexMode);
    }

    public static FCPAnalyzerProvider getFCPIndexAnalyzer(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        boolean indexMode = true;
        boolean useFirstPos = true;
        FCPAnalyzerProvider provider = new FCPAnalyzerProvider(indexSettings, env, name, settings, indexMode);
        FCPAnalyzer fcpAnalyzer = provider.get();
        fcpAnalyzer.setUseFirstPos(useFirstPos);
        return provider;
    }

    public static FCPAnalyzerProvider getFCPSearchAnalyzer(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        boolean indexMode = false;
        boolean useFirstPos = true;
        FCPAnalyzerProvider provider = new FCPAnalyzerProvider(indexSettings, env, name, settings, indexMode);
        FCPAnalyzer fcpAnalyzer = provider.get();
        fcpAnalyzer.setUseFirstPos(useFirstPos);
        return provider;
    }

    public static FCPAnalyzerProvider getLCPIndexAnalyzer(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        boolean indexMode = true;
        FCPAnalyzerProvider provider = new FCPAnalyzerProvider(indexSettings, env, name, settings, indexMode);
        FCPAnalyzer fcpAnalyzer = provider.get();
        fcpAnalyzer.setUseFirstPos(false);
        return provider;
    }

    public static FCPAnalyzerProvider getLCPSearchAnalyzer(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        boolean indexMode = false;
        FCPAnalyzerProvider provider = new FCPAnalyzerProvider(indexSettings, env, name, settings, indexMode);
        FCPAnalyzer fcpAnalyzer = provider.get();
        fcpAnalyzer.setUseFirstPos(false);
        return provider;
    }

    @Override
    public FCPAnalyzer get() {
        return analyzer;
    }
}