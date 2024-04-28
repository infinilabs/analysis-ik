package org.wltea.analyzer.fcp;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.wltea.analyzer.dic.DictSegment;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;
import org.wltea.analyzer.fcp.util.CharacterUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * use dict to extend terms
 */
public class ExtendFilter extends TokenFilter {
    // 默认入库模式
    public static final boolean DEFAULT_INDEX_MODE = true;
    // 默认对于特殊字符采用模糊搜索，扩大搜索范围
    public static final boolean DEFAULT_USELESS_MAPPING = true;
    // 默认对于句子的空白进行忽略
    public static final boolean DEFAULT_IGNORE_BLANK = true;
    // 默认使用 lcp 的模式，使用最后一个char的position
    public static final boolean DEFAULT_USE_FIRST_POSITION = false;
    // 在高亮的时候使用 offset
    public static final boolean DEFAULT_SHOW_OFFSET = false;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    // used for saving upstream tokens , implemented by Arraylist
    private List<TokenBody> tokenBodies = null;
    //use to save analyzed tokens ,use priority heap save order
    PriorityQueue<TokenBody> tokenResults = new PriorityQueue<TokenBody>(new Comparator<TokenBody>(){
        @Override
        public int compare(TokenBody o1, TokenBody o2){
//            return o1.position != o2.position ? Integer.compare(o1.position, o2.position) : Integer.compare(o2.startOffset, o1.startOffset);
            if(o1.position != o2.position) {
                return Integer.compare(o1.position, o2.position);
            } else if (o2.startOffset != o1.startOffset) {
                return Integer.compare(o2.startOffset, o1.startOffset);
            } else {
                return Integer.compare(o1.endOffset-o1.startOffset, o2.endOffset-o2.startOffset);
            }
        }
    });
    // 记录上一个 term 的position ，用于计算 positionIncrement
    private int prePosition = -1;

    private final boolean indexMode;
    // 对于上游的 分词结果 上个 end_offset 和 下一个 token的 start_offset 不相等。 像 “成 功” 之间有空格，该参数决定是否忽略空格组词， 默认为true，忽略之间的 空白
    private boolean ignoreBlank = true;
    // 是否使用 first char position ，默认使用，如果为 false，则变为 lcp_analyzer
    private boolean useFirstPos = true;
    // 特殊字符的映射，默认为 true 表示模糊匹配特殊字符。如果设置为 false ，将会把原始的char放到最终分词结果中。
    private boolean uselessMapping = true;
    // 入库模式下不显示，search 模式下显示offset，在 highlight 的时候也开启
    private boolean showOffset = false;


    public ExtendFilter setIgnoreBlank(boolean ignoreBlank) {
        this.ignoreBlank = ignoreBlank;
        return this;
    }


    public ExtendFilter setUseFirstPos(boolean useFirstPos) {
        this.useFirstPos = useFirstPos;
        return this;
    }

    public ExtendFilter setUselessMapping(boolean uselessMapping) {
        this.uselessMapping = uselessMapping;
        return this;
    }

    public ExtendFilter setShowOffset(boolean showOffset) {
        this.showOffset = showOffset;
        return this;
    }


    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    public ExtendFilter(TokenStream input) {
        this(input, DEFAULT_INDEX_MODE);
    }

    public ExtendFilter(TokenStream input, boolean indexMode) {
        super(input);
        this.indexMode = indexMode;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (tokenBodies == null && input.incrementToken()) {
            tokenBodies = new ArrayList<>();
            int position = -1;
            do {
                TokenBody tb= new TokenBody();
                // TODO lcp analyzer 入库的特殊处理方式(不支持 offset 和 term_vector 存储方式)，否则就要改变 lucene源码。
                tb.startOffset = showOffset ? offsetAtt.startOffset() : 0;
                tb.endOffset = showOffset ? offsetAtt.endOffset() : 0;
                // blank 类型会被舍弃，position不变
                tb.termBuffer = termAtt.toString();
                // 下面是处理 position 和 type的赋值
                if (CharacterUtil.CHAR_USELESS.equals(typeAtt.type())) {
                    if (isAllBlank(tb.termBuffer) && this.ignoreBlank) {
                        // 表示沿用上一个 position，下面将会被舍弃掉
                        tb.position = position;
                        tb.type = CharacterUtil.CHAR_BLANK;
                        tb.termBuffer = "";
                    } else {
                        position += posIncrAtt.getPositionIncrement();
                        tb.position = position;
                        tb.type = typeAtt.type();
                        if (uselessMapping) {
                            tb.termBuffer = "#"; // 无特殊含义，将特殊字符统一映射为 # 方便查询, 否则特殊字符也是需要精准匹配
                        }
                    }
                } else {
                    position += posIncrAtt.getPositionIncrement();
                    tb.position = position;
                    tb.type = typeAtt.type();
                }
                tokenBodies.add(tb);
            } while (input.incrementToken());

            extendTerms(tokenBodies, indexMode, ignoreBlank, useFirstPos);
        }
        if (tokenResults.size() > 0) {
            TokenBody body = tokenResults.poll();

            posIncrAtt.setPositionIncrement(body.position - prePosition);
            prePosition = body.position;
            char[] chars = body.termBuffer.toCharArray();
            termAtt.copyBuffer(chars, 0, chars.length);
            offsetAtt.setOffset(body.startOffset, body.endOffset);
            typeAtt.setType(body.type);
            return true;
        } else {
            tokenBodies = null;
            prePosition = -1;
        }
        return false;
    }


    /**
     * 判断参数是否全部由空白字符(空格、制表符、换行……)组成
     * @param s
     * @return
     */
    private boolean isAllBlank(String s) {
        return s.trim().length() == 0;
    }

    private void extendTerms(List<TokenBody> tokenBodies, boolean indexMode, boolean ignoreBlank, boolean useFirstPos) {
        if (tokenBodies == null || tokenBodies.size() == 0) {
            return;
        }
        for (int beginI = 0; beginI < tokenBodies.size(); beginI++) {
            TokenBody tokenBody = tokenBodies.get(beginI);
            if (!tokenBody.type.equals(CharacterUtil.CHAR_BLANK)) {
                // 处理当前char, 但要考虑向后扩展，得到以当前位置开始 以 endList 中位置结束的一系列term，
                List<Integer> endList = getCurrentEndList(tokenBodies, beginI, ignoreBlank);
                if (indexMode) {
                    tokenResults.add(tokenBody);
                    for (Integer endI : endList) {
                        TokenBody tb= new TokenBody();
                        tb.termBuffer = combineTermBuffer(tokenBodies, beginI, endI);
                        tb.startOffset = tokenBodies.get(beginI).startOffset;
                        tb.endOffset = tokenBodies.get(endI).endOffset;
                        if (useFirstPos) {
                            tb.position = tokenBodies.get(beginI).position;
                        } else {
                            tb.position = tokenBodies.get(endI).position;
                        }
                        tb.type = CharacterUtil.COMBINE_WORD;
                        tokenResults.add(tb);
                    }
                } else {
                    // 处理search analyzer 结果，贪婪向后匹配
                    // 1，只有单字，加入单字
                    // 2，有后缀匹配，采用最长的token结果(目的是找到个数最少的组合，非最优，但比较简单)
                    if (endList.isEmpty()) {
                        tokenResults.add(tokenBody); // 单字
                    } else {
                        int lastEnd = endList.get(endList.size()-1); // 取最长token
                        tokenBody.termBuffer = combineTermBuffer(tokenBodies, beginI, lastEnd);
                        tokenBody.startOffset = tokenBodies.get(beginI).startOffset;
                        tokenBody.endOffset = tokenBodies.get(lastEnd).endOffset;
                        if (useFirstPos) {
                            tokenBody.position = tokenBodies.get(beginI).position;
                        } else {
                            tokenBody.position = tokenBodies.get(lastEnd).position;
                        }
                        tokenBody.type = CharacterUtil.COMBINE_WORD;
                        tokenResults.add(tokenBody);

                        beginI = lastEnd;
                    }
                }
            }
        }
    }

    /**
     * 以 begin 开始，但是不包含 begin
     * @param tokenBodies
     * @param begin
     * @param ignoreBlank
     * @return
     */
    private List<Integer> getCurrentEndList(List<TokenBody> tokenBodies, int begin, boolean ignoreBlank) {
        List<Integer> endList = new ArrayList<>();
        DictSegment dict = Dictionary.getSingleton().get_MainDict();
        StringBuffer sb = new StringBuffer(tokenBodies.get(begin).termBuffer);
        for (int j = begin+1; j < tokenBodies.size(); j++) {
            TokenBody current = tokenBodies.get(j);
            if (current.type.equals(CharacterUtil.CHAR_BLANK)) {
                if(ignoreBlank) {
                    continue;
                } else {
                    break;
                }
            }
            // 处理 中文情况
            sb.append(current.termBuffer);
            Hit hit = dict.match(sb.toString().toCharArray());
            if (hit.isUnmatch()) {
                break;
            }
            if (hit.isMatch()) {
                endList.add(j);
            }
        }
//        System.out.println(endList);
        return endList;
    }

    /**
     * 拼接 [begin, end] termBuffer
     * @param tokenBodies
     * @param begin
     * @param end
     * @return
     */
    private String combineTermBuffer(List<TokenBody> tokenBodies, int begin, int end) {
        StringBuffer sb = new StringBuffer(tokenBodies.get(begin).termBuffer);
        for(int i = begin+1; i <= end; i++) {
            sb.append(tokenBodies.get(i).termBuffer);
        }
        return sb.toString();
    }

}
