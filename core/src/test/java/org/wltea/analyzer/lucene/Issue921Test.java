package org.wltea.analyzer.lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wltea.analyzer.TestUtils;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Issue #921 测试：使用远程停用词表导致 FVH 高亮器对多值字段高亮偏移
 * https://github.com/infinilabs/analysis-ik/issues/921
 *
 * 核心问题：当多值字段中某个值全部被停用词过滤时，
 * IKTokenizer.end() 返回的 finalOffset 为 0 而非实际长度，
 * 导致后续值的 offset 累积错误，FVH 高亮偏移。
 */
public class Issue921Test {

    private static Configuration cfgMaxWord;
    private static Configuration cfgSmart;

    /**
     * 初始化配置并将 "value" 添加到停用词字典
     */
    @BeforeClass
    public static void setUp() throws Exception {
        cfgMaxWord = TestUtils.createFakeConfigurationSub(false);
        cfgSmart = TestUtils.createFakeConfigurationSub(true);
        addStopword("value");
    }

    /**
     * 通过反射向 Dictionary 的停用词字典树中添加一个词
     */
    private static void addStopword(String word) throws Exception {
        Field stopWordsField = Dictionary.class.getDeclaredField("_StopWords");
        stopWordsField.setAccessible(true);
        Object stopWords = stopWordsField.get(Dictionary.getSingleton());
        Method fillSegment = stopWords.getClass().getDeclaredMethod("fillSegment", char[].class);
        fillSegment.setAccessible(true);
        fillSegment.invoke(stopWords, (Object) word.toLowerCase().toCharArray());
    }

    /**
     * 辅助方法：收集所有 token 及其 offset 和 positionIncrement
     */
    static List<TokenInfo> tokenizeWithDetails(Configuration cfg, String text) {
        List<TokenInfo> tokens = new ArrayList<>();
        try (IKAnalyzer ikAnalyzer = new IKAnalyzer(cfg)) {
            TokenStream tokenStream = ikAnalyzer.tokenStream("text", text);
            tokenStream.reset();

            CharTermAttribute charTermAttr = tokenStream.getAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAttr = tokenStream.getAttribute(OffsetAttribute.class);
            PositionIncrementAttribute posIncrAttr = tokenStream.getAttribute(PositionIncrementAttribute.class);
            TypeAttribute typeAttr = tokenStream.getAttribute(TypeAttribute.class);

            while (tokenStream.incrementToken()) {
                String term = charTermAttr.toString();
                int startOffset = offsetAttr.startOffset();
                int endOffset = offsetAttr.endOffset();
                int posIncr = posIncrAttr.getPositionIncrement();
                String type = typeAttr.type();
                tokens.add(new TokenInfo(term, startOffset, endOffset, posIncr, type));
            }
            tokenStream.end();

            int finalOffset = offsetAttr.startOffset();
            tokens.add(new TokenInfo("<FINAL_OFFSET>", finalOffset, finalOffset, 0, "META"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tokens;
    }

    /**
     * 获取 finalOffset（调用 end() 后的 offset）
     */
    static int getFinalOffset(Configuration cfg, String text) {
        try (IKAnalyzer ikAnalyzer = new IKAnalyzer(cfg)) {
            TokenStream tokenStream = ikAnalyzer.tokenStream("text", text);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                // 消费所有 token
            }
            tokenStream.end();
            OffsetAttribute offsetAttr = tokenStream.getAttribute(OffsetAttribute.class);
            return offsetAttr.startOffset();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==========================================
    // 测试用例
    // ==========================================

    /**
     * 测试1: 单值字段全是停用词 - finalOffset 应正确
     * "value" 被过滤后不应产生任何 token，但 finalOffset 应为 5
     */
    @Test
    public void testSingleStopwordValue_finalOffset() {
        String text = "value";

        List<TokenInfo> tokens = tokenizeWithDetails(cfgMaxWord, text);

        System.out.println("=== testSingleStopwordValue_finalOffset ===");
        System.out.println("Input: \"value\"");
        for (TokenInfo t : tokens) {
            System.out.println("  " + t);
        }

        // "value" 是停用词，不应产生任何实质 token
        List<String> realTokens = tokens.stream()
                .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                .map(TokenInfo::getTerm)
                .collect(Collectors.toList());
        assert realTokens.isEmpty() : "\"value\" 是停用词，不应产生分词结果，实际: " + realTokens;

        // 关键验证：finalOffset 应为 5（"value" 的长度）
        TokenInfo finalToken = tokens.stream()
                .filter(t -> t.term.equals("<FINAL_OFFSET>"))
                .findFirst()
                .orElse(null);
        assert finalToken != null : "应有 FINAL_OFFSET token";
        assert finalToken.startOffset == 5 :
                "finalOffset 应为 5，实际为 " + finalToken.startOffset;
    }

    /**
     * 测试2: 文本中间有停用词 - offset 和 positionIncrement 正确
     * "hello value world" 中 "value" 被过滤
     */
    @Test
    public void testStopwordInMiddle_offsetAndPosition() {
        String text = "hello value world";

        List<TokenInfo> tokens = tokenizeWithDetails(cfgMaxWord, text);

        System.out.println("=== testStopwordInMiddle_offsetAndPosition ===");
        System.out.println("Input: " + text);
        for (TokenInfo t : tokens) {
            System.out.println("  " + t);
        }

        List<String> terms = tokens.stream()
                .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                .map(TokenInfo::getTerm)
                .collect(Collectors.toList());

        assert terms.contains("hello") : "应包含 'hello'";
        assert terms.contains("world") : "应包含 'world'";
        assert !terms.contains("value") : "不应包含 'value'";

        // 验证 offset
        TokenInfo helloToken = tokens.stream().filter(t -> t.term.equals("hello")).findFirst().orElse(null);
        assert helloToken != null;
        assert helloToken.startOffset == 0 : "hello startOffset 应为 0";
        assert helloToken.endOffset == 5 : "hello endOffset 应为 5";

        TokenInfo worldToken = tokens.stream().filter(t -> t.term.equals("world")).findFirst().orElse(null);
        assert worldToken != null;
        assert worldToken.startOffset == 12 : "world startOffset 应为 12，实际为 " + worldToken.startOffset;
        assert worldToken.endOffset == 17 : "world endOffset 应为 17";

        // 验证 "world" 的 positionIncrement 应为 2（跳过了 "value"）
        assert worldToken.posIncrement == 2 :
                "'world' 的 positionIncrement 应为 2（跳过了 'value'），实际为 " + worldToken.posIncrement;

        // 验证 finalOffset
        TokenInfo finalToken = tokens.stream().filter(t -> t.term.equals("<FINAL_OFFSET>")).findFirst().orElse(null);
        assert finalToken != null;
        assert finalToken.startOffset == 17 : "finalOffset 应为 17";
    }

    /**
     * 测试3: 中文停用词
     * 手动添加 "的" 为停用词，验证中文停用词过滤
     */
    @Test
    public void testChineseStopword() throws Exception {
        addStopword("的");
        String text = "我的数据库";

        List<TokenInfo> tokens = tokenizeWithDetails(cfgMaxWord, text);

        System.out.println("=== testChineseStopword ===");
        System.out.println("Input: " + text);
        for (TokenInfo t : tokens) {
            System.out.println("  " + t);
        }

        List<String> terms = tokens.stream()
                .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                .map(TokenInfo::getTerm)
                .collect(Collectors.toList());

        assert !terms.contains("的") : "'的' 是停用词，不应出现";
        assert terms.contains("我") : "应包含 '我'";
        assert terms.contains("数据库") : "应包含 '数据库'";
    }

    /**
     * 测试4: 复现 issue #921 原始场景
     * 模拟多值数组 ["RS", "复称", "value", "数据", "采集", "232", "485", "数据库", "数据库服务器"]
     * 其中 "value" 是停用词，验证所有值的 finalOffset 正确
     */
    @Test
    public void testOriginalIssueScenario_multiValueOffsets() {
        String[] values = {"RS", "复称", "value", "数据", "采集", "232", "485", "数据库", "数据库服务器"};
        // 每个值的期望 finalOffset = 值的字符串长度
        int[] expectedLengths = {2, 2, 5, 2, 2, 3, 3, 3, 6};

        System.out.println("=== testOriginalIssueScenario_multiValueOffsets ===");

        // 累积 offset（模拟 ES 多值字段索引）
        int cumulativeOffset = 0;
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            int expectedLength = expectedLengths[i];

            int actualFinalOffset = getFinalOffset(cfgMaxWord, value);
            cumulativeOffset += actualFinalOffset;

            System.out.println("  Value " + (i + 1) + ": \"" + value + "\" (len=" + value.length() + ")");
            System.out.println("    finalOffset: " + actualFinalOffset + " (expected: " + expectedLength + ")");
            System.out.println("    cumulativeOffset: " + cumulativeOffset);

            assert actualFinalOffset == expectedLength :
                    "值 " + (i + 1) + " (\"" + value + "\") finalOffset 错误: 期望 " + expectedLength + "，实际 " + actualFinalOffset;
        }

        // 最终累积 offset 应该等于所有值长度之和
        int expectedTotal = 0;
        for (int len : expectedLengths) {
            expectedTotal += len;
        }
        System.out.println("Total cumulative offset: " + cumulativeOffset + " (expected: " + expectedTotal + ")");
        assert cumulativeOffset == expectedTotal :
                "总累积 offset 错误: 期望 " + expectedTotal + "，实际 " + cumulativeOffset;
    }

    /**
     * 测试5: 无停用词场景 - 确保行为不变（回归测试）
     */
    @Test
    public void testNoStopword_regression() {
        String text = "中华人民共和国";

        List<TokenInfo> tokens = tokenizeWithDetails(cfgMaxWord, text);

        System.out.println("=== testNoStopword (regression) ===");
        System.out.println("Input: " + text);
        for (TokenInfo t : tokens) {
            System.out.println("  " + t);
        }

        List<String> terms = tokens.stream()
                .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                .map(TokenInfo::getTerm)
                .collect(Collectors.toList());
        assert !terms.isEmpty();

        // 验证最后一个 token 的 endOffset 和 finalOffset 一致
        TokenInfo lastRealToken = tokens.get(tokens.size() - 2);
        TokenInfo finalToken = tokens.get(tokens.size() - 1);
        assert finalToken.startOffset >= lastRealToken.endOffset :
                "finalOffset 应 >= 最后一个 token 的 endOffset";
    }

    /**
     * 测试6: ik_smart 模式下停用词过滤
     */
    @Test
    public void testSmartModeStopword() {
        String text = "hello value world";

        List<TokenInfo> tokens = tokenizeWithDetails(cfgSmart, text);

        System.out.println("=== testSmartModeStopword ===");
        System.out.println("Input: " + text);
        for (TokenInfo t : tokens) {
            System.out.println("  " + t);
        }

        List<String> terms = tokens.stream()
                .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                .map(TokenInfo::getTerm)
                .collect(Collectors.toList());
        assert terms.contains("hello") : "应包含 'hello'";
        assert terms.contains("world") : "应包含 'world'";
        assert !terms.contains("value") : "不应包含 'value'";

        // 验证 finalOffset
        TokenInfo finalToken = tokens.stream().filter(t -> t.term.equals("<FINAL_OFFSET>")).findFirst().orElse(null);
        assert finalToken != null;
        assert finalToken.startOffset == 17 : "finalOffset 应为 17";
    }

    /**
     * 测试7: 连续停用词 - positionIncrement 正确累加
     */
    @Test
    public void testConsecutiveStopwords() throws Exception {
        addStopword("hello");
        addStopword("world");

        String text = "hello a the world test";

        List<TokenInfo> tokens = tokenizeWithDetails(cfgMaxWord, text);

        System.out.println("=== testConsecutiveStopwords ===");
        System.out.println("Input: " + text);
        for (TokenInfo t : tokens) {
            System.out.println("  " + t);
        }

        // "hello", "a", "the", "world" 都是停用词，只有 "test" 不是
        List<String> terms = tokens.stream()
                .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                .map(TokenInfo::getTerm)
                .collect(Collectors.toList());
        assert !terms.contains("hello") : "不应包含 'hello'";
        assert !terms.contains("a") : "不应包含 'a'";
        assert !terms.contains("the") : "不应包含 'the'";
        assert !terms.contains("world") : "不应包含 'world'";
        assert terms.contains("test") : "应包含 'test'";

        // "test" 的 positionIncrement 应为 5（跳过了4个停用词）
        TokenInfo testToken = tokens.stream().filter(t -> t.term.equals("test")).findFirst().orElse(null);
        assert testToken != null;
        assert testToken.posIncrement == 5 :
                "'test' 的 positionIncrement 应为 5（跳过了4个停用词），实际为 " + testToken.posIncrement;
    }

    static class TokenInfo {
        private final String term;
        private final int startOffset;
        private final int endOffset;
        private final int posIncrement;
        private final String type;

        public TokenInfo(String term, int startOffset, int endOffset, int posIncrement, String type) {
            this.term = term;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.posIncrement = posIncrement;
            this.type = type;
        }

        public String getTerm() { return term; }
        public int getStartOffset() { return startOffset; }
        public int getEndOffset() { return endOffset; }
        public int getPosIncrement() { return posIncrement; }
        public String getType() { return type; }

        @Override
        public String toString() {
            return term + "[" + startOffset + "," + endOffset + "] posIncr=" + posIncrement + " type=" + type;
        }
    }
}
