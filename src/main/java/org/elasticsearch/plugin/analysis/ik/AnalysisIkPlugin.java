package org.elasticsearch.plugin.analysis.ik;

import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.IkAnalysisBinderProcessor;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


public class AnalysisIkPlugin extends Plugin {

    @Override public String name() {
        return "ik";
    }

    @Override public String description() {
        return "ik analysis";
    }

    @Override
    public Collection<Module> nodeModules() {
        return Collections.<Module>singletonList(new ConfiguredIkModule());
    }

    public void onModule(AnalysisModule module) {
        module.addProcessor(new IkAnalysisBinderProcessor());
    }

    public static class ConfiguredIkModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(AnalysisIkConfiguration.class).asEagerSingleton();
           /* Multibinder<AbstractCatAction> catActionMultibinder = Multibinder.newSetBinder(binder(), AbstractCatAction.class);
            catActionMultibinder.addBinding().to(ExampleCatAction.class).asEagerSingleton();*/
        }
    }
}