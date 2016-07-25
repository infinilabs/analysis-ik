package org.elasticsearch.indices.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.analysis.AnalyzerScope;
import org.elasticsearch.index.analysis.PreBuiltAnalyzerProviderFactory;
import org.elasticsearch.index.analysis.PreBuiltTokenizerFactoryFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.wltea.analyzer.lucene.IKTokenizer;

/**
 * Registers indices level analysis components so, if not explicitly configured,
 * will be shared among all indices.
 */
public class IKIndicesAnalysis extends AbstractComponent {

    private boolean useSmart=false;

    @Inject
    public IKIndicesAnalysis(final Settings settings,
                                   IndicesAnalysisService indicesAnalysisService,Environment env) {
        super(settings);
        final Configuration configuration=new Configuration(env,settings).setUseSmart(false);
        final Configuration smartConfiguration=new Configuration(env,settings).setUseSmart(true);

        indicesAnalysisService.analyzerProviderFactories().put("ik",
                new PreBuiltAnalyzerProviderFactory("ik", AnalyzerScope.GLOBAL,
                        new IKAnalyzer(configuration)));

        indicesAnalysisService.analyzerProviderFactories().put("ik_smart",
                new PreBuiltAnalyzerProviderFactory("ik_smart", AnalyzerScope.GLOBAL,
                        new IKAnalyzer(smartConfiguration)));

        indicesAnalysisService.analyzerProviderFactories().put("ik_max_word",
                new PreBuiltAnalyzerProviderFactory("ik_max_word", AnalyzerScope.GLOBAL,
                        new IKAnalyzer(configuration)));

        indicesAnalysisService.tokenizerFactories().put("ik",
                new PreBuiltTokenizerFactoryFactory(new TokenizerFactory() {
                    @Override
                    public String name() {
                        return "ik";
                    }

                    @Override
                    public Tokenizer create() {
                        return new IKTokenizer(configuration);
                    }
                }));

        indicesAnalysisService.tokenizerFactories().put("ik_smart",
                new PreBuiltTokenizerFactoryFactory(new TokenizerFactory() {
                    @Override
                    public String name() {
                        return "ik_smart";
                    }

                    @Override
                    public Tokenizer create() {
                        return new IKTokenizer(smartConfiguration);
                    }
                }));

        indicesAnalysisService.tokenizerFactories().put("ik_max_word",
                new PreBuiltTokenizerFactoryFactory(new TokenizerFactory() {
                    @Override
                    public String name() {
                        return "ik_max_word";
                    }

                    @Override
                    public Tokenizer create() {
                        return new IKTokenizer(configuration);
                    }
                }));
    }
}
