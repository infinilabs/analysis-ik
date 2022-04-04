package org.elasticsearch.dic.action.ik;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Created on 2022/4/1.
 *
 * @author lan
 */
public class DicAddRequestBuilder extends ActionRequestBuilder<DicAddRequest, DicAddResponse, DicAddRequestBuilder> {



    protected DicAddRequestBuilder(ElasticsearchClient client) {
        super(client, DicAddAction.INSTANCE, new DicAddRequest());
    }
}
