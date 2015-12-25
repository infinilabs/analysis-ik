package org.elasticsearch.index.analysis;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.settings.IndexSettingsService;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.lucene.IKAnalyzer;

@Deprecated
public class IkAnalyzerProvider extends AbstractIndexAnalyzerProvider<IKAnalyzer> {
    private final IKAnalyzer analyzer;
    private boolean useSmart=false;

    @Inject
    public IkAnalyzerProvider(Index index, IndexSettingsService indexSettingsService, Environment env, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettingsService.getSettings(), name, settings);
        Dictionary.initial(new Configuration(env));
        useSmart = settings.get("use_smart", "false").equals("true");
        analyzer=new IKAnalyzer(useSmart);
    }

    @Override public IKAnalyzer get() {
        return this.analyzer;
    }
}
