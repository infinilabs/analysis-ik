package org.wltea.analyzer.fcp;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.wltea.analyzer.fcp.util.CharacterUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * combine continues english or number
 */
public class CombineCharFilter extends TokenFilter {
    public static final int DEFAULT_MAX_WORD_LEN = 255;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

    // used for saving upstream tokens , implemented by Arraylist
    private List<TokenBody> tokenBodies = null;
    private Queue<TokenBody> tokenResults = new LinkedList();
    // token 最大长度。防止过长English
    private final int maxTokenLen;

    private static final Set<String> numberDot;
    static {
        Set<String> tmp = new HashSet<>();
        tmp.add("."); // 2.345
        tmp.add(","); // 1,234,567
        numberDot = Collections.unmodifiableSet(tmp);
    }

    public CombineCharFilter(TokenStream input) {
        super(input);
        this.maxTokenLen = DEFAULT_MAX_WORD_LEN;
    }
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     * @param maxTokenLen
     */
    public CombineCharFilter(TokenStream input, int maxTokenLen) {
        super(input);
        this.maxTokenLen = maxTokenLen;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (tokenBodies == null && input.incrementToken()) {
            tokenBodies = new ArrayList<>();
            do {
                TokenBody tb = new TokenBody(
                        termAtt.toString(),
                        offsetAtt.startOffset(),
                        offsetAtt.endOffset(),
                        typeAtt.type());
                tokenBodies.add(tb);
            } while (input.incrementToken());

            combineCharsByType(tokenBodies);
        }
        if (tokenResults.size() > 0) {
            TokenBody body = tokenResults.poll();
            char[] chars = body.termBuffer.toCharArray();
            termAtt.copyBuffer(chars, 0, chars.length);
            offsetAtt.setOffset(body.startOffset, body.endOffset);
            typeAtt.setType(body.type);
            posIncrAtt.setPositionIncrement(1);
            return true;
        } else {
            tokenBodies = null;
        }
        return false;
    }

    private void combineCharsByType(List<TokenBody> tokenBodies) {
        if (tokenBodies == null || tokenBodies.size() == 0) {
            return;
        }
        // 处理合并 english number useless
        List<TokenBody> sameType = new ArrayList<>();
        for (int beginI = 0; beginI < tokenBodies.size();) {
            int nextTypeIndex = getNextTypeIndex(tokenBodies, beginI);
            TokenBody body = composeTokens(tokenBodies, beginI, nextTypeIndex, tokenBodies.get(beginI).type);
            sameType.add(body);
            beginI = nextTypeIndex;
        }
        // 继续处理 english number
        for (int beginI = 0; beginI < sameType.size();) {
            TokenBody current = sameType.get(beginI);
            int nextI = beginI + 1;
            if (CharacterUtil.CHAR_NUMBER.equals(current.type) || CharacterUtil.CHAR_ENGLISH.equals(current.type)) {
                for(; nextI < sameType.size(); nextI++) {
                    TokenBody next = sameType.get(nextI);
                    if (CharacterUtil.CHAR_NUMBER.equals(next.type)
                            || CharacterUtil.CHAR_ENGLISH.equals(next.type)) {
                        current.type = CharacterUtil.ALPHANUM;
                        current.termBuffer = current.termBuffer + next.termBuffer;
                        current.endOffset = next.endOffset;
                    } else {
                        break;
                    }
                }
            }
            beginI = nextI;
            tokenResults.add(current);
        }

    }

    private TokenBody composeTokens(List<TokenBody> tokenBodies, int beginI, int nextTypeIndex, String type) {
        StringBuffer buffer = new StringBuffer();
        int startOffset = tokenBodies.get(beginI).startOffset;
        int endOffset = tokenBodies.get(nextTypeIndex - 1).endOffset;
        for(int i = beginI; i < nextTypeIndex; i++) {
            buffer.append(tokenBodies.get(i).termBuffer);
        }
        return new TokenBody(buffer.toString(), startOffset, endOffset, type);
    }

    // 首 TokenBody 的 type 作为整体
    private int getNextTypeIndex(List<TokenBody> tokenBodies,final int beginI) {
        int currentIndex = beginI;
        // 如果 currentIndex 为 tokenBodies 的最后一个位置，直接返回
        if (currentIndex == tokenBodies.size() - 1) {
            return currentIndex + 1;
        }
        TokenBody current = tokenBodies.get(currentIndex);
        final String currentWordType = current.type;
        int maxIndex = Math.min(currentIndex + maxTokenLen, tokenBodies.size());
        if (CharacterUtil.CHAR_NUMBER.equals(currentWordType)) {
            for (currentIndex++; currentIndex < maxIndex; currentIndex++) {
                current = tokenBodies.get(currentIndex);
                if (CharacterUtil.CHAR_USELESS.equals(current.type) && numberDot.contains(current.termBuffer)) {
                    if (currentIndex+1 < maxIndex && CharacterUtil.CHAR_NUMBER.equals(tokenBodies.get(currentIndex+1).type)) {
                        // 改变了整体的 type
                        tokenBodies.get(beginI).type = CharacterUtil.CHAR_NUMBER_DOT;
                    } else {
                        break;
                    }
                } else if (!CharacterUtil.CHAR_NUMBER.equals(current.type)) {
                    break;
                }
            }
            return currentIndex;
        } else if (CharacterUtil.CHAR_ENGLISH.equals(currentWordType) || CharacterUtil.CHAR_USELESS.equals(currentWordType)) {
            for (currentIndex++; currentIndex < maxIndex; currentIndex++) {
                current = tokenBodies.get(currentIndex);
                if (!currentWordType.equals(current.type)) {
                    break;
                }
            }
            return currentIndex;
        } else {
            return currentIndex + 1;
        }
    }


    private static class TokenBody {
        String termBuffer;
        int startOffset, endOffset;
        String type;

        TokenBody(String termBuffer, int startOffset, int endOffset, String type){
            this.termBuffer = termBuffer;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.type = type;
        }
    }
}
