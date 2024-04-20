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
import org.wltea.analyzer.fcp.tokenattributes.PositionLengthAttribute;
import org.wltea.analyzer.fcp.util.CharacterUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * use dict to extend terms
 */
public class ExtendFilter extends TokenFilter {
    private static final boolean IS_DEBUG = true;
    // 默认入库模式
    public static final boolean DEFAULT_INDEX_MODE = true;
    // 默认对于特殊字符采用模糊搜索，扩大搜索范围
    public static final boolean DEFAULT_USELESS_MAPPING = true;
    // 默认对于句子的空白进行忽略
    public static final boolean DEFAULT_IGNORE_BLANK = true;
    // 默认对于句子的空白进行忽略
    public static final boolean DEFAULT_IGNORE_WHITESPACE = true;
    // 默认使用 lcp 的模式，使用最后一个char的position
    public static final boolean DEFAULT_USE_FIRST_POSITION = false;
    // 在高亮的时候使用 offset
    public static final boolean DEFAULT_SHOW_OFFSET = false;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    // 用于记录每一个term 的position length
    private final PositionLengthAttribute lengthAttribute = addAttribute(PositionLengthAttribute.class);

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
                // 下面是处理 position 和 type的赋值，单个 term，没有 startPosition 和 endPosition
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
                            tb.termBuffer = "#"; // 无特殊含义，将特殊字符统一映射为 # 方便查询
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
            if (!indexMode) {
                // 计算当前combine term 的跨度，占用了多少个 term
                lengthAttribute.setPositionLength(body.endPosition - body.startPosition + 1);
            }
            return true;
        } else {
            tokenBodies = null;
            prePosition = -1;
        }
        return false;
    }


    /**
     * 判断参数是否全部由空白字符组成
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
                // 默认在 index 模式下，一股脑全部放到倒排中（index 模式对性能敏感，所以必须保证）
                if (!indexMode) {
                    tokenBody.startPosition = tokenBody.position;
                    tokenBody.endPosition = tokenBody.position;
                }
                tokenResults.add(tokenBody);
                for (Integer endI : endList) {
                    TokenBody tb= new TokenBody();
                    tb.termBuffer = combineTermBuffer(tokenBodies, beginI, endI);
                    tb.startOffset = tokenBodies.get(beginI).startOffset;
                    tb.endOffset = tokenBodies.get(endI).endOffset;
                    // search 模式下需要记录组合 term  前后的 position
                    if (!indexMode) {
                        tb.startPosition = tokenBodies.get(beginI).position;
                        tb.endPosition = tokenBodies.get(endI).position;
                    }
                    if (useFirstPos) {
                        tb.position = tokenBodies.get(beginI).position;
                    } else {
                        tb.position = tokenBodies.get(endI).position;
                    }
                    tb.type = "<COMBINE_WORD>";
                    tokenResults.add(tb);
                }
            }
        }
        // 到这里如果是index 模式的话，已经可以结束了；
        // 如果是 search模式，需要做歧义处理(如果有的话， 使用 <CHAR_USELESS> 类型的char 作为天然分割句子)
        if (!indexMode && tokenResults.size() > 0) {
            // 在search 模式下，采用 ik_smart 的逻辑进行语义分割，一个重大的意义：引入了语义分割
            // 1，ik 使用没有语义重叠的那个 char 作为分割点，只作用于有字符重叠的部分
            // 2，由于和 index 模式使用相同的 向后扩展逻辑，所以search是index 的子集
            // 3，search 模式下，不会涉及mapping的扩展引入
            // 4，search 模式下，使用 startPosition 来进行判断扩是否有歧义

            // 用于保存多个term的组合形式，逆序。：采用动态编程思想，完成快速组合
            PriorityQueue<TokenBody> combineTerms = new PriorityQueue<TokenBody>(new Comparator<TokenBody>(){
                @Override
                public int compare(TokenBody o1, TokenBody o2){
                    // 顺序有重要意义
                    return o1.startPosition != o2.startPosition ?
                            Integer.compare(o1.startPosition, o2.startPosition)
                            : Integer.compare(o2.endPosition, o1.endPosition);
                }
            });
            // 用于保存单个term的形式（最后将保存全部的结果）
            Map<Integer, TokenBody> singleTerm = new HashMap<>();

            // 将切分结果重新排序, 并清空之前的处理结果
            int startPosition = Integer.MAX_VALUE;
            int endPosition = Integer.MIN_VALUE;
            while (tokenResults.size() > 0) {
                TokenBody t = tokenResults.poll();
                if (t.startPosition == t.endPosition) {
                    // 单个 term
                    singleTerm.put(t.position, t);
                    startPosition = Math.min(startPosition, t.startPosition);
                    endPosition = Math.max(endPosition, t.endPosition);
                } else {
                    // 组合出来的term，不参与歧义判断，仅仅用于歧义判断后的填补那些空白的 position
                    combineTerms.add(t);
                }
            }

            // 处理分词，没有歧义的直接放到结果中，有歧义的处理完之后放到结果中
            PriorityQueue<TokenBody> searchReverseOrder = new PriorityQueue<TokenBody>(new Comparator<TokenBody>(){
                @Override
                public int compare(TokenBody o1, TokenBody o2){
                    // 顺序有重要意义
                    return o1.startPosition != o2.startPosition ?
                            Integer.compare(o2.startPosition, o1.startPosition)
                            : Integer.compare(o1.endPosition, o2.endPosition);
                }
            });

            // 在处理一段歧义时，控制前后范围， 第一次就是最开始的范围
            int maxExtend = Integer.MIN_VALUE;            // 边界包含
            for (TokenBody tb : combineTerms) {
                if (searchReverseOrder.size() == 0) {
                    searchReverseOrder.add(tb);
                    maxExtend = tb.endPosition;
                    continue;
                }

                if (maxExtend < tb.startPosition) {
                    // 表示当前term 与之前的切分没有歧义
                    if (searchReverseOrder.size() == 1) {
                        final TokenBody body = searchReverseOrder.poll();
                        singleTerm.put(body.startPosition, body);
                    } else {
                        // 这里先处理掉之前有歧义的部分，
                        final List<TokenBody> arbitrator = arbitrator(searchReverseOrder);
                        for(TokenBody body : arbitrator) {
                            singleTerm.put(body.startPosition, body);
                        }
                    }
                }
                searchReverseOrder.add(tb);
                maxExtend = Math.max(maxExtend, tb.endPosition);
            }
            // 处理最后的歧义
            if (searchReverseOrder.size() == 1) {
                final TokenBody body = searchReverseOrder.poll();
                singleTerm.put(body.startPosition, body);
            } else if(searchReverseOrder.size() > 1){
                final List<TokenBody> arbitrator = arbitrator(searchReverseOrder);
                for(TokenBody body : arbitrator) {
                    singleTerm.put(body.startPosition, body);
                }
            }
            // endPosition 的用途
            while (startPosition <= endPosition) {
                if (singleTerm.containsKey(startPosition)) {
                    final TokenBody body = singleTerm.get(startPosition);
                    tokenResults.add(body);
                    startPosition = body.endPosition + 1;
                } else {
                    startPosition++;
                }
            }
        }
    }

    /**
     * 处理有歧义的token，
     * @param searchReverseOrder  为倒序的token
     * @return
     */
    private List<TokenBody> arbitrator(PriorityQueue<TokenBody> searchReverseOrder) {
        Map<Integer, List<TokenBody>> positionMap = new HashMap<>();
        int maxIndex = -1;
        int minIndex = -1;
        while (searchReverseOrder.size() > 0) {
            final TokenBody body = searchReverseOrder.poll();
            if (searchReverseOrder.size() == 0) {
                // 要处理的最开始的位置，也就是 searchReverseOrder 的最后一个
                minIndex = body.startPosition;
            }
            if (maxIndex == -1) {
                // 要处理的最后的位置，也就是 searchReverseOrder 的第一个
                maxIndex = body.startPosition;
            }
            // 下面给当前的 token 添加 child
            int currentMax = maxIndex;
            for (int i = body.endPosition + 1; i <= currentMax; i++) {
                if (positionMap.containsKey(i)) {
                    final List<TokenBody> bodies = positionMap.get(i);
                    final TokenBody minLengthBody = bodies.get(0); // 表示取其后紧挨着的最短token作为结束位置
                    if (currentMax == maxIndex) {
                        currentMax = minLengthBody.endPosition; // 表示 minLengthBody 后面的 term 不可以作为 child了
                    }
                    if (body.child == null) {
                        body.child = new ArrayList<>();
                    }
                    body.child.addAll(positionMap.get(i));
                }
            }
            // 将 token放到结果中
            if (positionMap.containsKey(body.startPosition) == false) {
                positionMap.put(body.startPosition, new ArrayList<>());
            }
            positionMap.get(body.startPosition).add(body);

//            if (IS_DEBUG) {
//                for(int i = 0; i < maxIndex + 10; i++) {
//                    String s = "- ";
//                    if (body.startPosition <= i && i <= body.endPosition) {
//                        s = "# ";
//                    }
//                    System.out.print(s);
//                }
//                System.out.println();
//            }
        }
        List<TokenBody> topOptions = new ArrayList<>();

        final TokenBody firstMinLength = positionMap.get(minIndex).get(0);
        for(int i = firstMinLength.startPosition; i <= firstMinLength.endPosition; i++) {
            if (positionMap.containsKey(i)) {
                topOptions.addAll(positionMap.get(i));
            }
        }
        for (TokenBody t : topOptions) {
            System.out.println(t);
        }
        List<TokenBody> result = new ArrayList<>();
        final OptionPath bestPath = chooseBestPath(topOptions);
        for (int i = 0; i < bestPath.size ; i++) {
            int startP = bestPath.getValueByIndex(2 * i);
            int endP = bestPath.getValueByIndex(2 * i + 1);
            final List<TokenBody> bodyList = positionMap.get(startP);
            for(TokenBody tb : bodyList) {
               if (tb.startPosition == startP && tb.endPosition == endP) {
                   result.add(tb);
                   break;
               }
            }
        }
        return result;
    }

    // options 本身为已经处理好的结构，使用引用指向下级关系
    private OptionPath chooseBestPath(List<TokenBody> options) {
        // 使用 PriorityQueue，因为只是需要获取最小的那一个，其后的严格有序不是必须的
        PriorityQueue<OptionPath> allOptionPath = new PriorityQueue<OptionPath>(new Comparator<OptionPath> () {
            @Override
            public int compare(OptionPath o1, OptionPath o2) {
                return o2.compareTo(o1);
            }
        });

        for(TokenBody tokenBody : options) {
            OptionPath path = new OptionPath();
            path.addElement(tokenBody.startPosition, tokenBody.endPosition);
            findNextPath(allOptionPath, tokenBody, path);
        }
        final OptionPath bestPath = allOptionPath.poll();
        return bestPath;
    }

    private void findNextPath(PriorityQueue<OptionPath> allOptionPath, TokenBody tokenBody, OptionPath parentPath) {
        if (tokenBody.child == null) {
            // 路径的最后，结束递归
            allOptionPath.add(parentPath);
            return;
        }
        for(TokenBody child : tokenBody.child) {
            // 复制parent path
            OptionPath childPath = parentPath.copy();
            childPath.addElement(child.startPosition, child.endPosition);
            findNextPath(allOptionPath, child, childPath);
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
