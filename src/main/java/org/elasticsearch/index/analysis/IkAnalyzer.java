package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.wltea.analyzer.lucene.IKTokenizer;

import java.io.Reader;


public class IkAnalyzer extends Analyzer {
   
    @Override public TokenStream tokenStream(String fieldName, Reader reader) {            
        return new IKTokenizer(reader,true);
    }
    
  
    public IkAnalyzer() {
        super(); 
    }
}
