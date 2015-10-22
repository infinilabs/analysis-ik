package org.elasticsearch.plugin.analysis.ik;

import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.IkAnalysisBinderProcessor;
import org.elasticsearch.plugins.Plugin;


public class AnalysisIkPlugin extends Plugin {

    @Override public String name() {
        return "ik";
    }


    @Override public String description() {
        return "ik analysis";
    }

    /*@Override
    public Collection<Module> nodeModules() {
        return super.nodeModules();
    }*/

    public void onModule(AnalysisModule module) {
        module.addProcessor(new IkAnalysisBinderProcessor());
    }
}