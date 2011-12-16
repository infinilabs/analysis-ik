/**
 * 
 */
package org.wltea.analyzer.lucene;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.wltea.analyzer.IKSegmentation;
import org.wltea.analyzer.Lexeme;



public final class IKTokenizer extends Tokenizer {
	
	private IKSegmentation _IKImplement;
	private TermAttribute termAtt;
	private OffsetAttribute offsetAtt;
	private int finalOffset;


	public IKTokenizer(Reader in , boolean isMaxWordLength) {
	    super(in);
	    offsetAtt = addAttribute(OffsetAttribute.class);
	    termAtt = addAttribute(TermAttribute.class);
		_IKImplement = new IKSegmentation(in , isMaxWordLength);
	}	
	
	@Override
	public final boolean incrementToken() throws IOException {

		clearAttributes();
		Lexeme nextLexeme = _IKImplement.next();
		if(nextLexeme != null){

			termAtt.setTermBuffer(nextLexeme.getLexemeText());

			termAtt.setTermLength(nextLexeme.getLength());

			offsetAtt.setOffset(nextLexeme.getBeginPosition(), nextLexeme.getEndPosition());

			finalOffset = nextLexeme.getEndPosition();

			return true;
		}

		return false;
	}
	
	
	public void reset(Reader input) throws IOException {
		super.reset(input);
		_IKImplement.reset(input);
	}	
	
	@Override
	public final void end() {

		offsetAtt.setOffset(finalOffset, finalOffset);
	}
	
}
