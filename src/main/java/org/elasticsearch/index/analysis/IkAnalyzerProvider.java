package org.elasticsearch.index.analysis;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class IkAnalyzerProvider extends AbstractIndexAnalyzerProvider<IKAnalyzer> {
    private final IKAnalyzer analyzer;
    private boolean useSmart=false;

    @Inject
    public IkAnalyzerProvider(Index index, Settings indexSettings,Environment env, String name, Settings settings) {
        super(index, indexSettings, name, settings);
        Dictionary.initial(new Configuration(env));
        useSmart = settings.get("use_smart", "false").equals("true");
        analyzer=new IKAnalyzer(useSmart);
    }

    @Override public IKAnalyzer get() {
        return this.analyzer;
    }
}
