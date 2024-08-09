package com.infinilabs.ik.elasticsearch;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.lucene.IKAnalyzer;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;

public class Test {
    public static void main(String[] args) throws IOException {
        String configPath = "E:\\temp\\config";
        Settings settings = Settings.builder()
                .put("path.home",configPath)
                .put("path.data",configPath)
                .put("path.logs",configPath)
                .put("path.repo",configPath)
                .put("path.shared_data",configPath)
                .build();
        Environment env =new Environment(settings, FileSystems.getDefault().getPath(configPath));

        Configuration cfg = new ConfigurationSub(env, settings);
        try (IKAnalyzer ikAnalyzer = new IKAnalyzer(cfg)) {
            org.apache.lucene.analysis.TokenStream tokenStream = ikAnalyzer.tokenStream("text","剃\uDB84\uDC97鬚髪。或見菩\uDB84\uDCA7");
            tokenStream.reset();

            while(tokenStream.incrementToken())
            {
                CharTermAttribute charTermAttribute = tokenStream.getAttribute(CharTermAttribute.class);
                char[] chars = charTermAttribute.buffer();
                System.out.println(charTermAttribute.toString()+"-"+String.join(",",convertCharArrayToHex(chars)));
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
}
