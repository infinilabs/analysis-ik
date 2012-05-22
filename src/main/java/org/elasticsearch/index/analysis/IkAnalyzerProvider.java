package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.settings.IndexSettings;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

public class IkAnalyzerProvider extends AbstractIndexAnalyzerProvider<IKAnalyzer> {
    private final IKAnalyzer analyzer;
    private ESLogger logger=null;
    @Inject
    public IkAnalyzerProvider(Index index, @IndexSettings Settings indexSettings, Environment env, @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettings, name, settings);

//        logger = Loggers.getLogger("ik-analyzer");
//
//        logger.info("[Setting] {}",settings.getAsMap().toString());
//        logger.info("[Index Setting] {}",indexSettings.getAsMap().toString());
//        logger.info("[Env Setting] {}",env.configFile());

        analyzer=new IKAnalyzer(indexSettings);
    }

/*    @Override
    public String name() {
        return "ik";
    }

    @Override
    public AnalyzerScope scope() {
        return AnalyzerScope.INDEX;
    }*/


    public IkAnalyzerProvider(Index index, Settings indexSettings, String name,
    		Settings settings) {
		super(index, indexSettings, name, settings);
		analyzer=new IKAnalyzer(indexSettings);
	}

	public IkAnalyzerProvider(Index index, Settings indexSettings,
			String prefixSettings, String name, Settings settings) {
		super(index, indexSettings, prefixSettings, name, settings);
		analyzer=new IKAnalyzer(indexSettings);
	}


    @Override public IKAnalyzer get() {
        return this.analyzer;
    }
}
