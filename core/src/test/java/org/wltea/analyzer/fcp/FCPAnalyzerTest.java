package org.wltea.analyzer.fcp;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.Before;
import org.junit.Test;
import org.wltea.analyzer.dic.Dictionary;

import java.io.IOException;
import java.io.StringReader;

/**
 * @ClassName FCPAnalyzerTest
 * @Description: fcp test
 */
public class FCPAnalyzerTest {

    @Before
    public void init() {
        // 初始化词典
        Dictionary.initial(new Configuration4Test());
    }

    @Test
    public void testFcpIndexAnalyzer() {
        FCPAnalyzer fcpIndex = new FCPAnalyzer(true);
        String str = "这里是中国, this is china #4.345^";
        TokenStream stream = null ;
        try {
            stream = fcpIndex.tokenStream( "any", new StringReader(str)) ;
            PositionIncrementAttribute pia = stream.addAttribute(PositionIncrementAttribute.class ) ;  //保存位置
            OffsetAttribute oa = stream.addAttribute(OffsetAttribute.class ) ; //保存辞与词之间偏移量
            CharTermAttribute cta = stream.addAttribute(CharTermAttribute.class ) ;//保存响应词汇
            TypeAttribute ta = stream.addAttribute(TypeAttribute.class ) ; //保存类型
            stream.reset() ;
            int position = -1;
            while (stream.incrementToken()) {
                position += pia.getPositionIncrement();
                System. out.println(position + ":[" + cta.toString() + "]:" + oa.startOffset() + "->" + oa.endOffset() + ":" + ta.type());
            }
            stream.end() ;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testFcpSearchAnalyzer() {
        FCPAnalyzer fcpSearch = new FCPAnalyzer(false);
        String str = "这里是中国, this is china #4.345^";
        TokenStream stream = null ;
        try {
            stream = fcpSearch.tokenStream( "any", new StringReader(str)) ;
            PositionIncrementAttribute pia = stream.addAttribute(PositionIncrementAttribute.class ) ;  //保存位置
            OffsetAttribute oa = stream.addAttribute(OffsetAttribute.class ) ; //保存辞与词之间偏移量
            CharTermAttribute cta = stream.addAttribute(CharTermAttribute.class ) ;//保存响应词汇
            TypeAttribute ta = stream.addAttribute(TypeAttribute.class ) ; //保存类型
            stream.reset() ;
            int position = -1;
            while (stream.incrementToken()) {
                position += pia.getPositionIncrement();
                System. out.println(position + ":[" + cta.toString() + "]:" + oa.startOffset() + "->" + oa.endOffset() + ":" + ta.type());
            }
            stream.end() ;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void test03() {
        String s = " \t \n";
        System.out.println(s.trim().length() == 0);
    }
}
