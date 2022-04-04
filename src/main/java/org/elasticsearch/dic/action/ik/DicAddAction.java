package org.elasticsearch.dic.action.ik;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Created on 2022/4/1.
 *
 * @author lan
 */
public class DicAddAction extends Action<DicAddRequest, DicAddResponse, DicAddRequestBuilder> {

    public static final String NAME = "indices:data/write/add/dic";

    public static final DicAddAction INSTANCE = new DicAddAction();

    protected DicAddAction() {
        super(NAME);
    }

    @Override
    public DicAddRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new DicAddRequestBuilder(client);
    }

    @Override
    public DicAddResponse newResponse() {
        return new DicAddResponse();
    }
}
