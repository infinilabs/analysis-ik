package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.settings.IndexSettingsService;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.lucene.IKTokenizer;

@Deprecated
public class IkTokenizerFactory extends AbstractTokenizerFactory {
  private final Settings settings;
  private boolean useSmart=false;

  @Inject
  public IkTokenizerFactory(Index index, IndexSettingsService indexSettingsService,Environment env, @Assisted String name, @Assisted Settings settings) {
	  super(index, indexSettingsService.getSettings(), name, settings);
      this.settings=settings;
	  Dictionary.initial(new Configuration(env));
  }


  @Override
  public Tokenizer create() {
    this.useSmart = settings.get("use_smart", "false").equals("true");

    return new IKTokenizer(useSmart);  }
}
