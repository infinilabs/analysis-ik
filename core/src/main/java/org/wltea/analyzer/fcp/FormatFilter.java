package org.wltea.analyzer.fcp;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.wltea.analyzer.fcp.util.CharacterUtil;

import java.io.IOException;

/**
 * 英文转小写
 * 字符的类型处理
 */
public class FormatFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    public FormatFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            String s = termAtt.toString();
            // 如果从 ngram 1 的 Tokenizer 得到的 token 应该length 都为 1
            if (s.length() == 1) {
                int c = s.codePointAt(0);
                typeAtt.setType(CharacterUtil.identifyCharType(c));
                c = CharacterUtil.regularize(c);
                char[] chars = Character.toChars(c);
                termAtt.copyBuffer(chars, 0, chars.length);
            } else {
                // 对英文进行 lower case
                CharacterUtils.toLowerCase(termAtt.buffer(), 0, termAtt.length());
            }
            return true;
        } else {
            return false;
        }
    }

}
