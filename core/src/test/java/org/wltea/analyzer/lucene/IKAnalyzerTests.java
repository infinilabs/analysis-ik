package org.wltea.analyzer.lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import java.util.stream.Collectors;
import org.junit.Test;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.TestUtils;
import org.wltea.analyzer.core.Lexeme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IKAnalyzerTests {

    /**
     * 单char汉字+一个Surrogate Pair
     */
    @Test
    public void tokenizeCase1_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E");
        assert values.length == 2;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
    }

    /**
     * 单char汉字+一个Surrogate Pair+单char汉字
     */
    @Test
    public void tokenizeCase2_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E凤");
        assert values.length == 3;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
        assert values[2].equals("凤");
    }

    /**
     * 单char汉字和多Surrogate Pair混合
     */
    @Test
    public void tokenizeCase3_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E剃\uDB84\uDC97");
        assert values.length == 4;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
        assert values[2].equals("剃");
        assert values[3].equals("\uDB84\uDC97");
    }

    /**
     * 单char汉字和多个连续Surrogate Pair混合
     */
    @Test
    public void tokenizeCase4_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E\uDB84\uDC97");
        assert values.length == 3;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
        assert values[2].equals("\uDB84\uDC97");
    }

    /**
     * 单char汉字和多个连续Surrogate Pair加词库中的词
     */
    @Test
    public void tokenizeCase5_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E龟龙麟凤凤");
        assert values.length == 4;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
        assert values[2].equals("龟龙麟凤");
        assert values[3].equals("凤");
    }

    /**
     * Surrogate Pair混合超出缓存区测试
     */
    @Test
    public void tokenizeCase6_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        // build a string with '菩' + spaces + 60 surrogate pairs
        StringBuilder sb = new StringBuilder(4006);
        sb.append("菩");
        for (int i = 0; i < 3995; i++) {
            sb.append(' ');
        }
        // Append the surrogate pair 41 times
        for (int i = 0; i < 41; i++) {
            sb.append("\uDB84\uDD2E ");
        }
        String[] values = tokenize(cfg, sb.toString());
        
        // First token should be '菩'
        assert values[0].equals("菩");
        
        // There should be 41 tokens total (菩 + 41 surrogate pairs)
        assert values.length == 42;
        
        // Verify all surrogate pair tokens
        for (int i = 1; i <= 41; i++) {
            assert values[i].equals("\uDB84\uDD2E") : "Token at index " + i + " is not the expected surrogate pair";
        }
    }


    /**
     * 用ik_max_word分词器分词
     */
    @Test
    public void tokenize_max_word_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        List<String> values = Arrays.asList(tokenize(cfg, "中华人民共和国国歌"));
        assert values.size() >= 9;
        assert values.contains("中华人民共和国");
        assert values.contains("中华人民");
        assert values.contains("中华");
        assert values.contains("华人");
        assert values.contains("人民共和国");
        assert values.contains("人民");
        assert values.contains("共和国");
        assert values.contains("共和");
        assert values.contains("国歌");
    }

    /**
     * 用ik_smart分词器分词
     */
    @Test
    public void tokenize_smart_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(true);
        List<String> values = Arrays.asList(tokenize(cfg, "中华人民共和国国歌"));
        assert values.size() == 2;
        assert values.contains("中华人民共和国");
        assert values.contains("国歌");
    }

    static String[] tokenize(Configuration configuration, String s)
    {
        ArrayList<String> tokens = new ArrayList<>();
        try (IKAnalyzer ikAnalyzer = new IKAnalyzer(configuration)) {
            TokenStream tokenStream = ikAnalyzer.tokenStream("text", s);
            tokenStream.reset();

            while(tokenStream.incrementToken())
            {
                CharTermAttribute charTermAttribute = tokenStream.getAttribute(CharTermAttribute.class);
                OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
                int len = offsetAttribute.endOffset()-offsetAttribute.startOffset();
                char[] chars = new char[len];
                System.arraycopy(charTermAttribute.buffer(), 0, chars, 0, len);
                tokens.add(new String(chars));
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        return  tokens.toArray(new String[0]);
    }

    /**
     * 用ik_max_word分词器分词，测试中文量词
     */
    @Test
    public void tokenize_CN_Quantifier_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String text = "2023年人才";
        
        // 获取分词结果和类型
        List<TokenInfo> tokenInfos = tokenizeWithType(cfg, text);
        
        // 打印所有分词结果和类型，便于调试
        for (TokenInfo info : tokenInfos) {
            System.out.println("Token: " + info.getText() + ", Type: " + info.getType());
        }
        
        // 验证分词结果包含预期的词
        List<String> tokens = tokenInfos.stream().map(TokenInfo::getText).collect(Collectors.toList());
        assert tokens.contains("2023");
        assert tokens.contains("年");
        assert tokens.contains("人才");
        
        // 验证"人"不会被单独分割成COUNT类型
        boolean hasPersonAsCount = tokenInfos.stream()
                .anyMatch(info -> "人".equals(info.getText()) && info.getType() == Lexeme.TYPE_COUNT);
        assert !hasPersonAsCount : "'人'不应该被分割为COUNT类型";
        
        // 验证"年"是量词类型
        boolean hasYearAsCount = tokenInfos.stream()
                .anyMatch(info -> "年".equals(info.getText()) && info.getType() == Lexeme.TYPE_COUNT);
        assert hasYearAsCount : "'年'应该是COUNT类型";
    }


/**
     * 分词结果信息类，包含词文本和类型
     */
    static class TokenInfo {
        private String text;
        private int type;
        
        public TokenInfo(String text, int type) {
            this.text = text;
            this.type = type;
        }
        
        public String getText() {
            return text;
        }
        
        public int getType() {
            return type;
        }
    }
    
    /**
     * 获取分词结果及其类型信息
     */
    static List<TokenInfo> tokenizeWithType(Configuration configuration, String s) {
        ArrayList<TokenInfo> tokenInfos = new ArrayList<>();
        try (IKAnalyzer ikAnalyzer = new IKAnalyzer(configuration)) {
            TokenStream tokenStream = ikAnalyzer.tokenStream("text", s);
            tokenStream.reset();
            
            CharTermAttribute charTermAttribute = tokenStream.getAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
            TypeAttribute typeAttribute = tokenStream.getAttribute(TypeAttribute.class);
            
            while(tokenStream.incrementToken()) {
                int len = offsetAttribute.endOffset() - offsetAttribute.startOffset();
                char[] chars = new char[len];
                System.arraycopy(charTermAttribute.buffer(), 0, chars, 0, len);
                String text = new String(chars);
                
                // 获取类型信息并映射回对应的数字常量
                String typeStr = typeAttribute.type();
                int type = mapTypeStringToInt(typeStr);
                
                tokenInfos.add(new TokenInfo(text, type));
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return tokenInfos;
    }
    
    /**
     * 将类型字符串映射为对应的数字常量
     * 
     * @param typeStr 类型字符串
     * @return 对应的数字常量
     */
    private static int mapTypeStringToInt(String typeStr) {
        switch (typeStr) {
            case "ENGLISH":
                return Lexeme.TYPE_ENGLISH;
            case "ARABIC":
                return Lexeme.TYPE_ARABIC;
            case "LETTER":
                return Lexeme.TYPE_LETTER;
            case "CN_WORD":
                return Lexeme.TYPE_CNWORD;
            case "CN_CHAR":
                return Lexeme.TYPE_CNCHAR;
            case "OTHER_CJK":
                return Lexeme.TYPE_OTHER_CJK;
            case "COUNT":
                return Lexeme.TYPE_COUNT;
            case "TYPE_CNUM":
                return Lexeme.TYPE_CNUM;
            case "TYPE_CQUAN":
                return Lexeme.TYPE_CQUAN;
            default:
                return Lexeme.TYPE_UNKNOWN;
        }
    }
}
