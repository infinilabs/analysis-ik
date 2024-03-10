package com.infinilabs.ik.opensearch;

import org.apache.lucene.analysis.Tokenizer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenizerFactory;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.lucene.IKTokenizer;

public class IkTokenizerFactory extends AbstractTokenizerFactory {
  private Configuration configuration;

  public IkTokenizerFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
      super(indexSettings, settings,name);
      configuration = new ConfigurationSub(env,settings);
  }

  public static IkTokenizerFactory getIkTokenizerFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
      return new IkTokenizerFactory(indexSettings,env, name, settings).setSmart(false);
  }

  public static IkTokenizerFactory getIkSmartTokenizerFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
      return new IkTokenizerFactory(indexSettings,env, name, settings).setSmart(true);
  }

  public IkTokenizerFactory setSmart(boolean smart){
        this.configuration.setUseSmart(smart);
        return this;
  }

  @Override
  public Tokenizer create() {
      return new IKTokenizer(configuration);  }
}
