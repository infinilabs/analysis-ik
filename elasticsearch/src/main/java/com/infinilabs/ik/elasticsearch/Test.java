package com.infinilabs.ik.elasticsearch;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class Test {

    public static void main(String[] args) throws IOException {
        //todo: 完善更多单元测试
        Configuration cfg = new Test.FakeConfigurationSub();
        try (IKAnalyzer ikAnalyzer = new IKAnalyzer(cfg)) {
            org.apache.lucene.analysis.TokenStream tokenStream = ikAnalyzer.tokenStream("text","又見菩\uDB84\uDD2E，處林放光，濟地獄苦，令入佛\uDB84\uDC01。又見佛子\uD83D\uDE00\uD83D\uDE43龟龙麟凤剃\uDB84\uDC97鬚髪。或見菩\uDB84\uDCA7做张做势牛哈");
            tokenStream.reset();

            while(tokenStream.incrementToken())
            {
                CharTermAttribute charTermAttribute = tokenStream.getAttribute(CharTermAttribute.class);
                OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
                int len = offsetAttribute.endOffset()-offsetAttribute.startOffset();
                char[] chars = new char[len];
                System.arraycopy(charTermAttribute.buffer(), 0, chars, 0, len);
                TypeAttribute typeAttribute = tokenStream.getAttribute(TypeAttribute.class);
                System.out.println(charTermAttribute.toString()+"-"+typeAttribute.type()+"-"+String.join(",",convertCharArrayToHex(chars)));
            }
        }

    }

    public static String[] convertCharArrayToHex(char[] charArray) {
        ArrayList<String> hexList = new ArrayList<>(charArray.length);
        for (int i = 0; i < charArray.length; i++) {
            char ch = charArray[i];
            if(ch==0) break;
            hexList.add(String.format("%04x", (int) ch));
        }
        return hexList.toArray(new String[0]);
    }

    static class FakeConfigurationSub extends ConfigurationSub
    {
        public FakeConfigurationSub() {
            super(new Environment(getSettings(), getConfigDir()), getSettings());
        }

        @Override
        public Path getConfDir() {
            return getConfigDir();
        }

        @Override
        public Path getConfigInPluginDir() {
            return getConfigDir();
        }

        private static Path getConfigDir()
        {
            return new File(System.getProperty("user.dir"), "config").toPath();
        }

        private static Settings getSettings()
        {
            return Settings.builder()
                    .put("path.home",getConfigDir())
                    .build();
        }
    }
}
