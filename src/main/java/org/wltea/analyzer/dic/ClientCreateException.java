package org.wltea.analyzer.dic;

import java.io.IOException;

/**
 * @author nick.wn
 * @email nick.wn@alibaba-inc.com
 * @date 2018/9/27
 */
public class ClientCreateException extends IOException {
    public ClientCreateException(Throwable e) {
        super(e);
    }

    public ClientCreateException(String s) {
        super(s);
    }
}
