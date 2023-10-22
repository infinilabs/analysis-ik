package org.wltea.analyzer.lucene;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.junit.Before;
import org.junit.Test;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

public class IKAnalyzerTest {

    private static final Logger logger = ESPluginLoggerFactory.getLogger(IKAnalyzerTest.class.getName());
    private Configuration mockConfiguration;

    @Before
    public void setUp() {
        URL url = this
            .getClass().getResource("/elasticsearch/config/analysis-ik/IKAnalyzer.cfg.xml");
        logger.info("url={}", url);
        File esRootDir = new File(url.getFile()).getParentFile().getParentFile().getParentFile();
        logger.info("mock elasticsearch root dir={}", esRootDir);
        Settings settings = Settings.builder()
            .put("use_smart", "true")
            .put("enable_lowercase", "true")
            .put("enable_remote_dict", "false")
            .put("path.home", esRootDir.getAbsolutePath())
            .build();
        Environment env = new Environment(settings, new File(esRootDir, "config").toPath());
        mockConfiguration = new Configuration(env, settings);
    }

    @Test
    public void testCreateComponents() throws IOException {
        IKAnalyzer ikAnalyzerUnderTest = new IKAnalyzer(mockConfiguration);
        TokenStream tokenStream = ikAnalyzerUnderTest.tokenStream("fieldName", "测试一下分词效果，最近流行AA制，不知道是什么意思");
        tokenStream.reset();
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        while (tokenStream.incrementToken()) {
            System.out.println(charTermAttribute);
        }
        ikAnalyzerUnderTest.close();
    }

    @Test
    public void testGB18030_2022() throws IOException {
        List<String> list = new ArrayList<>();
        list.add(codePointsToString(0x20000));
        list.add(codePointsToString(0x2A6DF));
        Dictionary.getSingleton().addWords(list);
        StringBuilder sb = new StringBuilder("汉字");
        sb.appendCodePoint(0x3400).appendCodePoint(0x4DBF);
        sb.appendCodePoint(0x20000).appendCodePoint(0x2A6DF);
        sb.appendCodePoint(0x2A700).appendCodePoint(0x2B739);
        sb.appendCodePoint(0x2B740).appendCodePoint(0x2B81D);
        sb.appendCodePoint(0x2B820).appendCodePoint(0x2CEA1);
        sb.appendCodePoint(0x2CEB0).appendCodePoint(0x2EBE0);
        sb.appendCodePoint(0x30000).appendCodePoint(0x3134A);
        sb.appendCodePoint(0x31350).appendCodePoint(0x323AF);
        sb.appendCodePoint(0x2EBF0).appendCodePoint(0x2EE5D);
        System.out.println(sb.toString());
        IKAnalyzer ikAnalyzerUnderTest = new IKAnalyzer(mockConfiguration);
        TokenStream tokenStream = ikAnalyzerUnderTest.tokenStream("fieldName", sb.toString());
        tokenStream.reset();
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        while (tokenStream.incrementToken()) {
            System.out.println(charTermAttribute);
        }
        ikAnalyzerUnderTest.close();
        list = new ArrayList<>();
        list.add(codePointsToString(0x2A700, 0x2B739));
        Dictionary.getSingleton().addWords(list);

        ikAnalyzerUnderTest = new IKAnalyzer(mockConfiguration);
        TokenStream tokenStream2 = ikAnalyzerUnderTest.tokenStream("nf", sb.toString());
        tokenStream2.reset();
        charTermAttribute = tokenStream2.addAttribute(CharTermAttribute.class);
        while (tokenStream2.incrementToken()) {
            System.out.println("新的分词==" + charTermAttribute);
        }

    }

    /**
     * 将多个代码点生成字符串
     *
     * @param codePoints 多个合法的unicode代码点值， 0到10FFFF
     * @return 按代码点值得到的字符串
     */
    public static String codePointsToString(int... codePoints) {
        StringBuilder sb = new StringBuilder(16 + codePoints.length);
        for (int codePoint : codePoints) {
            sb.appendCodePoint(codePoint);
        }
        return toStringFix(sb);
    }

    /**
     * 性能优化后的StringBuilder toString
     *
     * @param sb 不能为null的StringBuilder
     * @return 如果StringBuilder里没有内容，则直接返回空白字符串，避免持有StringBuilder内的char数组
     */
    static String toStringFix(StringBuilder sb) {
        if (sb.length() < 1) {
            return "";
        }
        return sb.toString();
    }

}
