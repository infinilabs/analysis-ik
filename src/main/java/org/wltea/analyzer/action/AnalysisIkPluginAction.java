package org.wltea.analyzer.action;


import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

/**
 * @author nick.wn
 * @email nick.wn@alibaba-inc.com
 * @date 2018/10/18
 */
public class AnalysisIkPluginAction extends BaseRestHandler {

    private static final Logger logger = ESLoggerFactory.getLogger(AnalysisIkPluginAction.class.getName());

    /*@Inject
    public AnalysisIkPluginAction(final Settings settings, final RestController controller) {

        controller.registerHandler(RestRequest.Method.GET, "/hello", this);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        String who = request.param("who");
        String whoSafe = (who!=null) ? who : "world";
        channel.sendResponse(new BytesRestResponse(OK, "Hello, " + whoSafe + "!"));
    }*/


    @Inject
    public AnalysisIkPluginAction(final Settings settings, final RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.GET,"/hello", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String who = request.param("who");
        String whoSafe = (who!=null) ? who : "world";

        if (AnalysisIkPlugin.clusterService.state().nodes().getLocalNode() != null) {
            logger.info(String.format("node name is : " + AnalysisIkPlugin.clusterService.localNode().getName()));
        }
        return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.OK, "Hello, " + whoSafe + "!"));
    }
}
