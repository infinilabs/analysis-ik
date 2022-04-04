package org.elasticsearch.plugin.analysis.ik;

import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.dic.action.ik.DicAddAction;
import org.elasticsearch.dic.action.ik.DicAddTransportAction;
import org.elasticsearch.dic.action.ik.DicManager;
import org.elasticsearch.dic.action.ik.DicRestAction;
import org.elasticsearch.index.analysis.AnalyzerProvider;
import org.elasticsearch.index.analysis.IkAnalyzerProvider;
import org.elasticsearch.index.analysis.IkTokenizerFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.util.*;
import java.util.function.Supplier;


public class AnalysisIkPlugin extends Plugin implements AnalysisPlugin, ActionPlugin {

    public static String PLUGIN_NAME = "analysis-ik";

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> getTokenizers() {
        Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> extra = new HashMap<>();


        extra.put("ik_smart", IkTokenizerFactory::getIkSmartTokenizerFactory);
        extra.put("ik_max_word", IkTokenizerFactory::getIkTokenizerFactory);

        return extra;
    }

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> extra = new HashMap<>();

        extra.put("ik_smart", IkAnalyzerProvider::getIkSmartAnalyzerProvider);
        extra.put("ik_max_word", IkAnalyzerProvider::getIkAnalyzerProvider);

        return extra;
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Collections.singletonList(new ActionHandler<>(DicAddAction.INSTANCE, DicAddTransportAction.class));
    }

    @Override
    public List<Class<? extends ActionFilter>> getActionFilters() {
        return ActionPlugin.super.getActionFilters();
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings, IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter, IndexNameExpressionResolver indexNameExpressionResolver, Supplier<DiscoveryNodes> nodesInCluster) {
        return Collections.singletonList(new DicRestAction(settings, restController));
    }

    @Override
    public Collection<String> getRestHeaders() {
        return ActionPlugin.super.getRestHeaders();
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool, ResourceWatcherService resourceWatcherService, ScriptService scriptService, NamedXContentRegistry xContentRegistry) {
        DicManager instance = DicManager.INSTANCE;
        instance.init(client);
        return Collections.singletonList(instance);
    }
}
