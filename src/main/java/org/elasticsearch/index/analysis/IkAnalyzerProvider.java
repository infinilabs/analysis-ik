package org.elasticsearch.index.analysis;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.settings.IndexSettings;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class IkAnalyzerProvider extends AbstractIndexAnalyzerProvider<IKAnalyzer> {
    private final IKAnalyzer analyzer;
    @Inject
    public IkAnalyzerProvider(Index index, @IndexSettings Settings indexSettings, Environment env, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettings, name, settings);
        analyzer=new IKAnalyzer(indexSettings,settings);
    }

    public IkAnalyzerProvider(Index index, Settings indexSettings, String name,
    		Settings settings) {
		super(index, indexSettings, name, settings);
		analyzer=new IKAnalyzer(indexSettings,settings);
	}

	public IkAnalyzerProvider(Index index, Settings indexSettings,
			String prefixSettings, String name, Settings settings) {
		super(index, indexSettings, prefixSettings, name, settings);
		analyzer=new IKAnalyzer(indexSettings,settings);
	}


    @Override public IKAnalyzer get() {
        return this.analyzer;
    }
}
