package org.wltea.analyzer.dic.remote;

import org.wltea.analyzer.configuration.Configuration;

/**
 * AbstractRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:50
 */
public abstract class AbstractRemoteDictionary {

    protected final Configuration configuration;

    public AbstractRemoteDictionary(Configuration configuration) {
        this.configuration = configuration;
    }
}
