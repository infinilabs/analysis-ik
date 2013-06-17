package org.elasticsearch.index.analysis;


public class IkAnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {

    @Override public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {

    }


    @Override public void processAnalyzers(AnalyzersBindings analyzersBindings) {
        analyzersBindings.processAnalyzer("ik", IkAnalyzerProvider.class);
        super.processAnalyzers(analyzersBindings);
    }


    @Override
    public void processTokenizers(TokenizersBindings tokenizersBindings) {
      tokenizersBindings.processTokenizer("ik", IkTokenizerFactory.class);
      super.processTokenizers(tokenizersBindings);
    }
}
