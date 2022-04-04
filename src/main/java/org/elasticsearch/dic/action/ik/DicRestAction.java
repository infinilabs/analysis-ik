package org.elasticsearch.dic.action.ik;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 * Created on 2022/4/4.
 *
 * @author lan
 */
public class DicRestAction extends BaseRestHandler {

    public DicRestAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(POST, "/ik_dic", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String dicCode = null;
        String[] id = null;
        String[] item = null;

        if (request.hasContent()) {
            XContentParser parser = request.contentParser();
            XContentParser.Token token = parser.nextToken();
            if (token == null) {
                throw new IllegalArgumentException("No dicCode is specified");
            }
            String cfn = null;
            while ((token = parser.nextToken()) != null) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    cfn = parser.currentName();
                } else if (token.isValue() && "dicCode".equals(cfn)) {
                    dicCode = parser.text();
                } else if ("id".equals(cfn) && token == XContentParser.Token.START_ARRAY) {
                    List<String> idList = new ArrayList<>();
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        if (!token.isValue()) {
                            throw new IllegalArgumentException(cfn + " array element should only contain text");
                        }
                        idList.add(parser.text());
                    }
                    id = idList.toArray(new String[0]);
                } else if ("item".equals(cfn) && token == XContentParser.Token.START_ARRAY) {
                    List<String> itemList = new ArrayList<>();
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        if (!token.isValue()) {
                            throw new IllegalArgumentException(cfn + " array element should only contain text");
                        }
                        itemList.add(parser.text());
                    }
                    item = itemList.toArray(new String[0]);
                }
            }

        }

        if (dicCode == null || dicCode.isEmpty()) {
            throw new IllegalArgumentException("dicCode cannot be blank");
        }
        if (id == null || id.length == 0) {
            throw new IllegalArgumentException("id cannot be empty");
        }
        if (item == null || item.length == 0) {
            throw new IllegalArgumentException("item cannot be empty");
        }

        DicAddResponse dicAddResponse = DicManager.INSTANCE.addDicItem(dicCode, id, item);
        return restChannel -> restChannel.sendResponse(new BytesRestResponse(RestStatus.OK, dicAddResponse.isSuccess() ? "success" : "fail"))

                ;

    }


}
