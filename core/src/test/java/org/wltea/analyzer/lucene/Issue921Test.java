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
 *
 * 每个测试场景同时覆盖 ik_max_word 和 ik_smart 两种模式，交错展示。
 */
public class Issue921Test {

    private static Configuration cfgMaxWord;
    private static Configuration cfgSmart;

    @BeforeClass
    public static void setUp() throws Exception {
        cfgMaxWord = TestUtils.createFakeConfigurationSub(false);
        cfgSmart = TestUtils.createFakeConfigurationSub(true);
        addStopword("value");
        addStopword("hello");
        addStopword("world");
        addStopword("的");
    }

    private static void addStopword(String word) throws Exception {
        Field stopWordsField = Dictionary.class.getDeclaredField("_StopWords");
        stopWordsField.setAccessible(true);
        Object stopWords = stopWordsField.get(Dictionary.getSingleton());
        Method fillSegment = stopWords.getClass().getDeclaredMethod("fillSegment", char[].class);
        fillSegment.setAccessible(true);
        fillSegment.invoke(stopWords, (Object) word.toLowerCase().toCharArray());
    }

    // ========== 工具方法 ==========

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
                tokens.add(new TokenInfo(
                        charTermAttr.toString(),
                        offsetAttr.startOffset(),
                        offsetAttr.endOffset(),
                        posIncrAttr.getPositionIncrement(),
                        typeAttr.type()));
            }
            tokenStream.end();

            int finalOffset = offsetAttr.startOffset();
            tokens.add(new TokenInfo("<FINAL_OFFSET>", finalOffset, finalOffset, 0, "META"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tokens;
    }

    static int getFinalOffset(Configuration cfg, String text) {
        try (IKAnalyzer ikAnalyzer = new IKAnalyzer(cfg)) {
            TokenStream tokenStream = ikAnalyzer.tokenStream("text", text);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {}
            tokenStream.end();
            return tokenStream.getAttribute(OffsetAttribute.class).startOffset();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void printHeader(String scenario, String mode, String description) {
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────────");
        System.out.println("│ 场景: " + scenario + " [" + mode + "]");
        System.out.println("│ 说明: " + description);
        System.out.println("└─────────────────────────────────────────────────────────");
    }

    private void printInput(String text) {
        System.out.println("  输入: \"" + text + "\" (长度=" + text.length() + ")");
    }

    private void printTokenTable(List<TokenInfo> tokens, String originalText) {
        List<TokenInfo> realTokens = tokens.stream()
                .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                .collect(Collectors.toList());
        TokenInfo finalOffsetToken = tokens.stream()
                .filter(t -> t.term.equals("<FINAL_OFFSET>"))
                .findFirst().orElse(null);

        // Token 明细表
        System.out.println("  ┌────────────────────┬──────────┬──────────┬──────────┬────────────┐");
        System.out.println("  │ term               │ start    │ end      │ posIncr  │ type       │");
        System.out.println("  ├────────────────────┼──────────┼──────────┼──────────┼────────────┤");
        for (TokenInfo t : realTokens) {
            System.out.printf("  │ %-18s │ %8d │ %8d │ %8d │ %-10s │%n",
                    t.term.length() > 18 ? t.term.substring(0, 15) + "..." : t.term,
                    t.startOffset, t.endOffset, t.posIncrement, t.type);
        }
        System.out.println("  └────────────────────┴──────────┴──────────┴──────────┴────────────┘");

        // 分词结果汇总
        if (originalText != null && !originalText.isEmpty()) {
            System.out.println("  原文:       \"" + originalText + "\"");

            StringBuilder mapLine = new StringBuilder();
            boolean[] covered = new boolean[originalText.length()];
            for (TokenInfo t : realTokens) {
                for (int i = t.startOffset; i < t.endOffset && i < originalText.length(); i++) {
                    covered[i] = true;
                }
            }
            for (int i = 0; i < originalText.length(); i++) {
                mapLine.append(covered[i] ? "^" : " ");
            }
            System.out.println("  词元覆盖:                 " + mapLine);

            String resultTerms = realTokens.stream()
                    .map(t -> "\"" + t.term + "\"")
                    .collect(Collectors.joining(", "));
            System.out.println("  分词结果:   [" + resultTerms + "] (" + realTokens.size() + "个词元)");

            // 未覆盖区域
            int uncoveredStart = -1;
            List<String> gaps = new ArrayList<>();
            for (int i = 0; i < originalText.length(); i++) {
                if (!covered[i]) {
                    if (uncoveredStart == -1) uncoveredStart = i;
                } else {
                    if (uncoveredStart != -1) {
                        gaps.add("\"" + originalText.substring(uncoveredStart, i).trim() + "\"");
                        uncoveredStart = -1;
                    }
                }
            }
            if (uncoveredStart != -1) {
                String remaining = originalText.substring(uncoveredStart).trim();
                if (!remaining.isEmpty()) gaps.add("\"" + remaining + "\"");
            }
            if (!gaps.isEmpty()) {
                System.out.println("  过滤区间:   " + String.join(", ", gaps) + " (停用词/空白)");
            }
        }

        if (finalOffsetToken != null) {
            System.out.println("  finalOffset: " + finalOffsetToken.startOffset);
        }
    }

    private void printAssertion(String label, Object expected, Object actual, boolean pass) {
        String icon = pass ? "✓" : "✗";
        System.out.println("  " + icon + " " + label + ": 期望=" + expected + ", 实际=" + actual);
    }

    private void printMultiValueResult(String[] values, int[] expectedLengths, int[] actualOffsets, int cumulativeOffset) {
        int expectedTotal = 0;
        for (int len : expectedLengths) expectedTotal += len;

        System.out.println("  ┌─────┬────────────────────┬──────┬──────────┬──────────┬───────┐");
        System.out.println("  │  #  │ 值                 │ 长度 │ 期望FO   │ 实际FO   │ 结果  │");
        System.out.println("  ├─────┼────────────────────┼──────┼──────────┼──────────┼───────┤");
        for (int i = 0; i < values.length; i++) {
            boolean pass = actualOffsets[i] == expectedLengths[i];
            String result = pass ? "  ✓  " : "  ✗  ";
            System.out.printf("  │ %3d │ %-18s │ %4d │ %8d │ %8d │ %s │%n",
                    i + 1,
                    values[i].length() > 18 ? values[i].substring(0, 15) + "..." : values[i],
                    values[i].length(), expectedLengths[i], actualOffsets[i], result);
        }
        System.out.println("  ├─────┼────────────────────┼──────┼──────────┼──────────┼───────┤");
        boolean totalPass = cumulativeOffset == expectedTotal;
        System.out.printf("  │     │ 累积 offset        │      │ %8d │ %8d │ %s │%n",
                expectedTotal, cumulativeOffset, totalPass ? "  ✓  " : "  ✗  ");
        System.out.println("  └─────┴────────────────────┴──────┴──────────┴──────────┴───────┘");
    }

    /** 对两种模式运行同一个验证逻辑 */
    private void runBothModes(String scenario, String description, BiConsumerWithException<Configuration, String> verifier) throws Exception {
        printHeader(scenario, "ik_max_word", description);
        verifier.accept(cfgMaxWord, "maxWord");
        printHeader(scenario, "ik_smart", description);
        verifier.accept(cfgSmart, "smart");
    }

    @FunctionalInterface
    interface BiConsumerWithException<T, U> {
        void accept(T t, U u) throws Exception;
    }

    // ========== 场景1: 全停用词 + 连续停用词 ==========

    @Test
    public void testAllStopwords() throws Exception {
        runBothModes("全停用词 + 连续停用词",
                "全部被过滤时 finalOffset 正确；连续停用词 posIncrement 累加",
                (cfg, mode) -> {
            // --- 单值全是停用词 ---
            String text = "value";
            List<TokenInfo> tokens = tokenizeWithDetails(cfg, text);
            printInput(text);
            printTokenTable(tokens, text);

            List<String> realTokens = tokens.stream()
                    .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                    .map(TokenInfo::getTerm).collect(Collectors.toList());
            boolean noTokens = realTokens.isEmpty();
            printAssertion("无 token 产出（停用词全部过滤）", true, noTokens, noTokens);
            assert noTokens : "[" + mode + "] \"value\" 是停用词，不应产生 token";

            int finalOffset = tokens.stream()
                    .filter(t -> t.term.equals("<FINAL_OFFSET>"))
                    .findFirst().map(TokenInfo::getStartOffset).orElse(-1);
            printAssertion("finalOffset = 文本长度", 5, finalOffset, finalOffset == 5);
            assert finalOffset == 5 : "[" + mode + "] finalOffset 应为 5，实际为 " + finalOffset;

            // --- 连续停用词 ---
            String text2 = "hello a the world test";
            List<TokenInfo> tokens2 = tokenizeWithDetails(cfg, text2);
            printInput(text2);
            printTokenTable(tokens2, text2);

            TokenInfo testToken = tokens2.stream()
                    .filter(t -> t.term.equals("test")).findFirst().orElse(null);
            assert testToken != null;
            boolean posCorrect = testToken.posIncrement == 5;
            printAssertion("'test' posIncrement（跳过 hello/a/the/world 4个停用词）", 5, testToken.posIncrement, posCorrect);
            assert posCorrect : "[" + mode + "] 'test' posIncrement 应为 5，实际为 " + testToken.posIncrement;

            int finalOffset2 = tokens2.stream()
                    .filter(t -> t.term.equals("<FINAL_OFFSET>"))
                    .findFirst().map(TokenInfo::getStartOffset).orElse(-1);
            printAssertion("finalOffset = 文本长度", 22, finalOffset2, finalOffset2 == 22);
            assert finalOffset2 == 22 : "[" + mode + "] finalOffset 应为 22";
        });
    }

    // ========== 场景2: 停用词过滤（英文+中文） ==========

    @Test
    public void testStopwordFiltering() throws Exception {
        runBothModes("停用词过滤（英文 offset + 中文停用词）",
                "英文停用词被过滤后 offset/posIncrement 正确；中文 '的' 被过滤",
                (cfg, mode) -> {
            // --- 英文 offset + positionIncrement ---
            String text = "foo value bar";
            List<TokenInfo> tokens = tokenizeWithDetails(cfg, text);
            printInput(text);
            printTokenTable(tokens, text);

            TokenInfo fooToken = tokens.stream().filter(t -> t.term.equals("foo")).findFirst().orElse(null);
            assert fooToken != null;
            printAssertion("'foo' startOffset", 0, fooToken.startOffset, fooToken.startOffset == 0);
            printAssertion("'foo' endOffset", 3, fooToken.endOffset, fooToken.endOffset == 3);
            printAssertion("'foo' posIncrement", 1, fooToken.posIncrement, fooToken.posIncrement == 1);
            assert fooToken.startOffset == 0 && fooToken.endOffset == 3 && fooToken.posIncrement == 1;

            TokenInfo barToken = tokens.stream().filter(t -> t.term.equals("bar")).findFirst().orElse(null);
            assert barToken != null;
            printAssertion("'bar' startOffset（跳过 'value ' 共6字符）", 10, barToken.startOffset, barToken.startOffset == 10);
            printAssertion("'bar' endOffset", 13, barToken.endOffset, barToken.endOffset == 13);
            printAssertion("'bar' posIncrement（跳过 value 1个停用词）", 2, barToken.posIncrement, barToken.posIncrement == 2);
            assert barToken.startOffset == 10 && barToken.endOffset == 13 && barToken.posIncrement == 2;

            // --- 中文停用词 ---
            String text2 = "我的数据库";
            List<TokenInfo> tokens2 = tokenizeWithDetails(cfg, text2);
            printInput(text2);
            printTokenTable(tokens2, text2);

            List<String> cnTerms = tokens2.stream()
                    .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                    .map(TokenInfo::getTerm).collect(Collectors.toList());
            printAssertion("'的' 不出现（停用词过滤）", false, cnTerms.contains("的"), !cnTerms.contains("的"));
            printAssertion("'我' 存在", true, cnTerms.contains("我"), cnTerms.contains("我"));
            printAssertion("'数据库' 存在", true, cnTerms.contains("数据库"), cnTerms.contains("数据库"));
            assert !cnTerms.contains("的") : "[" + mode + "] '的' 不应出现";
            assert cnTerms.contains("我") && cnTerms.contains("数据库");
        });
    }

    // ========== 场景3: 原始 issue 多值场景 ==========

    @Test
    public void testOriginalIssueScenario() throws Exception {
        runBothModes("原始 issue #921 多值场景",
                "模拟 ES 多值字段 [\"RS\",\"复称\",\"value\",\"数据\",\"采集\",\"232\",\"485\",\"数据库\",\"数据库服务器\"]，其中 \"value\" 是停用词",
                (cfg, mode) -> {
            String[] values = {"RS", "复称", "value", "数据", "采集", "232", "485", "数据库", "数据库服务器"};
            int[] expectedLengths = {2, 2, 5, 2, 2, 3, 3, 3, 6};
            int[] actualOffsets = new int[values.length];

            int cumulativeOffset = 0;
            for (int i = 0; i < values.length; i++) {
                actualOffsets[i] = getFinalOffset(cfg, values[i]);
                cumulativeOffset += actualOffsets[i];
                assert actualOffsets[i] == expectedLengths[i] :
                        "[" + mode + "] 值 " + (i + 1) + " finalOffset: 期望 " + expectedLengths[i] + "，实际 " + actualOffsets[i];
            }

            int expectedTotal = 0;
            for (int len : expectedLengths) expectedTotal += len;
            printMultiValueResult(values, expectedLengths, actualOffsets, cumulativeOffset);

            assert cumulativeOffset == expectedTotal :
                    "[" + mode + "] 总累积 offset: 期望 " + expectedTotal + "，实际 " + cumulativeOffset;

            System.out.println("  ※ 值3 \"value\" 全部是停用词，修复前 finalOffset=0，修复后 finalOffset=5");
            System.out.println("  ※ 修复前累积 offset 差 5 字符，导致后续所有值的高亮偏移");
        });
    }

    // ========== 场景4: 无停用词回归测试 ==========

    @Test
    public void testNoStopwordRegression() throws Exception {
        runBothModes("无停用词回归测试",
                "确保修复不影响正常分词场景",
                (cfg, mode) -> {
            String text = "中华人民共和国";
            List<TokenInfo> tokens = tokenizeWithDetails(cfg, text);
            printInput(text);
            printTokenTable(tokens, text);

            List<String> terms = tokens.stream()
                    .filter(t -> !t.term.equals("<FINAL_OFFSET>"))
                    .map(TokenInfo::getTerm).collect(Collectors.toList());
            assert !terms.isEmpty() : "[" + mode + "] 应有分词结果";

            TokenInfo lastRealToken = tokens.get(tokens.size() - 2);
            TokenInfo finalToken = tokens.get(tokens.size() - 1);
            boolean pass = finalToken.startOffset >= lastRealToken.endOffset;
            printAssertion("finalOffset >= 最后 token endOffset",
                    ">=" + lastRealToken.endOffset, finalToken.startOffset, pass);
            assert pass : "[" + mode + "] finalOffset 应 >= 最后一个 token 的 endOffset";
        });
    }

    // ========== 场景5: 多值+值内含停用词 ==========

    @Test
    public void testMultiValueWithPartialStopword() throws Exception {
        runBothModes("多值+值内含停用词",
                "多值字段 [\"RS\",\"foo value bar\",\"数据库\"]，\"value\" 被过滤但 finalOffset 仍正确",
                (cfg, mode) -> {
            String[] values = {"RS", "foo value bar", "数据库"};
            int[] expectedLengths = {2, 13, 3};

            int cumulativeOffset = 0;
            int[] actualOffsets = new int[values.length];

            for (int i = 0; i < values.length; i++) {
                List<TokenInfo> tokens = tokenizeWithDetails(cfg, values[i]);
                int actualFinalOffset = tokens.stream()
                        .filter(t -> t.term.equals("<FINAL_OFFSET>"))
                        .findFirst().map(TokenInfo::getStartOffset).orElse(-1);
                actualOffsets[i] = actualFinalOffset;
                cumulativeOffset += actualFinalOffset;
                assert actualFinalOffset == expectedLengths[i] :
                        "[" + mode + "] 值 " + (i + 1) + " finalOffset: 期望 " + expectedLengths[i] + "，实际 " + actualFinalOffset;
            }

            printMultiValueResult(values, expectedLengths, actualOffsets, cumulativeOffset);

            int expectedTotal = 0;
            for (int len : expectedLengths) expectedTotal += len;
            assert cumulativeOffset == expectedTotal :
                    "[" + mode + "] 总累积 offset: 期望 " + expectedTotal + "，实际 " + cumulativeOffset;

            // "foo value bar" 内部细节
            System.out.println("  ── \"foo value bar\" 内部分词详情 ──");
            List<TokenInfo> midTokens = tokenizeWithDetails(cfg, "foo value bar");
            printTokenTable(midTokens, "foo value bar");

            TokenInfo barToken = midTokens.stream().filter(t -> t.term.equals("bar")).findFirst().orElse(null);
            assert barToken != null;
            boolean offsetPass = barToken.startOffset == 10;
            boolean posPass = barToken.posIncrement == 2;
            printAssertion("'bar' startOffset（'value ' 被过滤，offset 不跳跃）", 10, barToken.startOffset, offsetPass);
            printAssertion("'bar' posIncrement（跳过 'value' 1个停用词）", 2, barToken.posIncrement, posPass);
            assert offsetPass && posPass;

            System.out.println("  ※ \"foo value bar\" 中 'value' 被过滤，但 offset 仍基于原始文本位置 [0,13]");
        });
    }

    // ========== Token 信息类 ==========

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
    }
}
