package org.wltea.analyzer.action;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.wltea.analyzer.dic.Dictionary;

/**
 * @author nick.wn
 * @email nick.wn@alibaba-inc.com
 * @date 2018/11/8
 */
public class AnalysisIkPluginAction extends BaseRestHandler {
    private static final Logger logger = ESLoggerFactory.getLogger(AnalysisIkPluginAction.class.getName());


    @Inject
    public AnalysisIkPluginAction(final Settings settings, final RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.GET,"/_aliyun_ik_plugin", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String who = request.param("who");
        String whoSafe = (who!=null) ? who : "world";
        StringBuilder sb = new StringBuilder();
        if (AnalysisIkPlugin.clusterService.state().nodes().getLocalNode() != null) {
            for (DiscoveryNode node : AnalysisIkPlugin.clusterService.state().nodes()) {
                sb.append("node name is :");
                sb.append(node.getName() + " ");
            }
            logger.info(sb);
        }

        sb.append("  oss ext" + Dictionary.getSingleton().getProperty("remote_oss_ext_dict"));
        sb.append(" oss stop word" + Dictionary.getSingleton().getProperty("remote_oss_ext_stopwords"));
        return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.OK, "Hello, " + whoSafe + "!" + " NodesInfo is " + sb.toString()));
    }
}
