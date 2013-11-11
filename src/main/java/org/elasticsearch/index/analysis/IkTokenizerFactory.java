package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.settings.IndexSettings;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.lucene.IKTokenizer;

import java.io.Reader;

public class IkTokenizerFactory extends AbstractTokenizerFactory {
  private Environment environment;
  private Settings settings;

  @Inject
  public IkTokenizerFactory(Index index, @IndexSettings Settings indexSettings, Environment env, @Assisted String name, @Assisted Settings settings) {
	  super(index, indexSettings, name, settings);
	  this.environment = env;
	  this.settings = settings;
	  Dictionary.initial(new Configuration(env));
  }

  @Override
  public Tokenizer create(Reader reader) {
	  return new IKTokenizer(reader, settings, environment);
  }

}
