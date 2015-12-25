package org.elasticsearch.index.analysis;


@Deprecated
public class IkAnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {


    @Override
    public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {

    }


    @Override
    public void processAnalyzers(AnalyzersBindings analyzersBindings) {
        analyzersBindings.processAnalyzer("ik", IkAnalyzerProvider.class);
    }


    @Override
    public void processTokenizers(TokenizersBindings tokenizersBindings) {
        tokenizersBindings.processTokenizer("ik", IkTokenizerFactory.class);
    }
}
