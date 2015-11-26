package org.elasticsearch.indices.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.AnalyzerScope;
import org.elasticsearch.index.analysis.PreBuiltAnalyzerProviderFactory;
import org.elasticsearch.index.analysis.PreBuiltTokenizerFactoryFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
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
                                   IndicesAnalysisService indicesAnalysisService) {
        super(settings);
        this.useSmart = settings.get("use_smart", "false").equals("true");
        indicesAnalysisService.analyzerProviderFactories().put("ik",
                new PreBuiltAnalyzerProviderFactory("ik", AnalyzerScope.GLOBAL,
                        new IKAnalyzer(useSmart)));

        indicesAnalysisService.analyzerProviderFactories().put("ik_smart",
                new PreBuiltAnalyzerProviderFactory("ik_smart", AnalyzerScope.GLOBAL,
                        new IKAnalyzer(true)));

        indicesAnalysisService.analyzerProviderFactories().put("ik_max_word",
                new PreBuiltAnalyzerProviderFactory("ik_max_word", AnalyzerScope.GLOBAL,
                        new IKAnalyzer(false)));

        indicesAnalysisService.tokenizerFactories().put("ik",
                new PreBuiltTokenizerFactoryFactory(new TokenizerFactory() {
                    @Override
                    public String name() {
                        return "ik";
                    }

                    @Override
                    public Tokenizer create() {
                        return new IKTokenizer(false);
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
                        return new IKTokenizer(true);
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
                        return new IKTokenizer(false);
                    }
                }));
    }
}