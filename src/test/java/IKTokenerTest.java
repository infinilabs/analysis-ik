/**
 *
 */

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.wltea.analyzer.lucene.IKTokenizer;


/**
 * @author 林良益
 *
 */
public class IKTokenerTest  {

	public void testLucene3Tokenizer(){
		String t = "IK分词器Lucene Analyzer接口实现类 民生银行";
		IKTokenizer tokenizer = new IKTokenizer(new StringReader(t) , false);
		try {
			while(tokenizer.incrementToken()){
				TermAttribute termAtt = tokenizer.getAttribute(TermAttribute.class);
				System.out.println(termAtt);
			}
		} catch (IOException e) {

			e.printStackTrace();
		}


	}



}
