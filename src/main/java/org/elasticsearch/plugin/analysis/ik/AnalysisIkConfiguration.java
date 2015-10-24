package org.elasticsearch.plugin.analysis.ik;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.analysis.AnalyzerScope;
import org.elasticsearch.index.analysis.PreBuiltAnalyzerProviderFactory;
import org.elasticsearch.index.analysis.PreBuiltTokenizerFactoryFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.wltea.analyzer.lucene.IKTokenizer;

public class AnalysisIkConfiguration extends AbstractComponent {

    private IndicesAnalysisService indicesAnalysisService;
    private Environment env;
    @Inject
    public AnalysisIkConfiguration(final Settings settings, IndicesAnalysisService indicesAnalysisService, final Environment env) {
        super(settings);
        this.indicesAnalysisService = indicesAnalysisService;
        this.env = env;
        Dictionary.initial(new Configuration(env));
        Register("ik", settings);
        Register("ik_smart", Settings.settingsBuilder().put(settings).put("use_smart", true).build());
    }

    private void Register(final String key, final Settings settings) {
        indicesAnalysisService.analyzerProviderFactories()
                .put(key, new PreBuiltAnalyzerProviderFactory(key, AnalyzerScope.GLOBAL, new IKAnalyzer(settings, env)));

        indicesAnalysisService.tokenizerFactories()
                .put(key, new PreBuiltTokenizerFactoryFactory(new TokenizerFactory() {

                    public String name() {
                        return key;
                    }

                    public Tokenizer create() {
                        return new IKTokenizer(settings, env);
                    }
                }));
    }

}
