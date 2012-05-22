/**
 * 
 */
package org.wltea.analyzer.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.wltea.analyzer.dic.Dictionary;

import java.io.Reader;

public final class IKAnalyzer extends Analyzer {
	
	private boolean isMaxWordLength = false;

	public IKAnalyzer(){
		this(false);
	}
	

	public IKAnalyzer(boolean isMaxWordLength){
		super();
		this.setMaxWordLength(isMaxWordLength);
	}

    public IKAnalyzer(Settings settings) {
       Dictionary.getInstance().Init(settings);
    }


    @Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		return new IKTokenizer(reader , isMaxWordLength());
	}

	public void setMaxWordLength(boolean isMaxWordLength) {
		this.isMaxWordLength = isMaxWordLength;
	}

	public boolean isMaxWordLength() {
		return isMaxWordLength;
	}

}
