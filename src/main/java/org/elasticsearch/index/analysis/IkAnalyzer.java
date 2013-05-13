package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.wltea.analyzer.lucene.IKTokenizer;
//import org.wltea.lucene.IKTokenizer;

import java.io.Reader;


public class IkAnalyzer extends Analyzer {
//    private boolean isMaxWordLength = false;
//    @Override public TokenStream tokenStream(String fieldName, Reader reader) {
//        return new IKTokenizer(reader,true);
//    }
    
  
    public IkAnalyzer() {
        super(); 
    }

    @Override
    protected TokenStreamComponents createComponents(String s, Reader reader) {
//        new TokenStreamComponents
        Tokenizer tokenizer = new IKTokenizer(reader, true);
        return new TokenStreamComponents(tokenizer, null);  //To change body of implemented methods use File | Settings | File Templates.
    }

//    public boolean isMaxWordLength() {
//        return isMaxWordLength;
//    }
}
