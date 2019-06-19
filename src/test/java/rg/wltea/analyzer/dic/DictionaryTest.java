/*
 * Copyright (c) 2019, guanquan.wang@yandex.com All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rg.wltea.analyzer.dic;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.junit.Before;
import org.junit.Test;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Create by guanquan.wang at 2019-06-14 11:14
 */
public class DictionaryTest {
    private Configuration config;

    @Before public void before() {

        Settings settings = Settings.builder()
            .put("use_smart", true)
            .put("enable_lowercase", true)
            .put("enable_remote_dict", true)
            .put("path.home", "")
            .build();

        config = new Configuration(new Environment(settings, testResourceRoot()), settings);
    }

    @Test public synchronized void testRunUnprivileged() throws InterruptedException {

        Dictionary.initial(config);
        this.wait();
    }


    // --- INNER STATIC FUNCTIONS ---

    public static Path testResourceRoot() {
        URL url = DictionaryTest.class.getClassLoader().getResource(".");
        if (url == null) {
            throw new RuntimeException("Load test resources error.");
        }
        return isWindows()
            ? Paths.get(url.getFile().substring(1))
            : Paths.get(url.getFile());
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toUpperCase().startsWith("WINDOWS");
    }
}
