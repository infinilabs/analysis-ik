package org.elasticsearch.dic.action.ik;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 * Created on 2022/4/1.
 *
 * @author lan
 */
public class DicAddTransportAction extends HandledTransportAction<DicAddRequest, DicAddResponse> {

    private final DicManager dicManager;

    @Inject
    public DicAddTransportAction(DicManager dicManager, Settings settings, ThreadPool threadPool, TransportService transportService, ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, DicAddAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, DicAddRequest::new);
        this.dicManager = dicManager;
    }

    @Override
    protected void doExecute(DicAddRequest request, ActionListener<DicAddResponse> listener) {
        try {
            DicAddResponse dicAddResponse = dicManager.addDicItem(request.getDicCode(), request.getId(), request.getItems());
            listener.onResponse(dicAddResponse);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}
