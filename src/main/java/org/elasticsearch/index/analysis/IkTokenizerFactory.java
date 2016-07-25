package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.settings.IndexSettingsService;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.lucene.IKTokenizer;

public class IkTokenizerFactory extends AbstractTokenizerFactory {
  private Configuration configuration;

  @Inject
  public IkTokenizerFactory(Index index, IndexSettingsService indexSettingsService,Environment env, @Assisted String name, @Assisted Settings settings) {
	  super(index, indexSettingsService.getSettings(), name, settings);
	  configuration=new Configuration(env,settings);
  }

  @Override
  public Tokenizer create() {
      return new IKTokenizer(configuration);  }
}
