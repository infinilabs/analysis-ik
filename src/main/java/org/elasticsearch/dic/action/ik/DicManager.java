package org.elasticsearch.dic.action.ik;

import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.wltea.analyzer.dic.Dictionary;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created on 2022/4/1.
 *
 * @author lan
 */
public class DicManager {

    public final static DicManager INSTANCE = new DicManager();

    private Client client;

    public static final String _index = "ik_dic_index";

    public static final String _type = "doc";


    public void init(Client client) {
        this.client = client;
    }

    public DicAddResponse addDicItem(String dicCode, String[] ids, String[] items) {
        if (items.length != ids.length) {
            throw new IllegalArgumentException("the length of ids and items must equal!");
        }
        if (items.length == 1) {
            IndexResponse indexResponse = buildRequest(ids[0], dicCode, items[0]).get();

        } else {
            BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
            for (int i = 0; i < items.length; i++) {
                bulkRequestBuilder.add(buildRequest(ids[i], dicCode, items[i]));
            }
            BulkResponse response = bulkRequestBuilder.get();
        }
        Dictionary.getSingleton().addWords(dicCode, items);
        return new DicAddResponse().setSuccess(true);
    }

    private IndexRequestBuilder buildRequest(String _id, String dicCode, String item) {
        Map<String, String> source = new HashMap<>(4);
        source.put("dicCode", dicCode);
        source.put("item", item);

        return client.prepareIndex(_index, _type, _id)
                .setSource(source)
                .setOpType(DocWriteRequest.OpType.INDEX);
    }

    public List<String> listDictItems(String dicCode) {
        List<String> res = new ArrayList<>();

        SearchResponse response = client.prepareSearch(_index)
                .setTypes(_type)
                .setQuery(
                        QueryBuilders.termQuery("dicCode", dicCode)
                )
                .setSize(10000)
                .setScroll(new TimeValue(60000))
                .get();

        SearchHit[] hits;
        do {
            hits = response.getHits().getHits();

            res.addAll(
                    Stream.of(hits)
                            .map(searchHitFields -> searchHitFields.getSourceAsMap().get("item"))
                            .map(o -> (String) o)
                            .filter(o -> Objects.nonNull(o) && !o.isEmpty())
                            .collect(Collectors.toList())
            );
            response =
                    client.prepareSearchScroll(response.getScrollId())
                            .setScroll(new TimeValue(60000))
                            .get();

        } while (hits.length != 0);

        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.setScrollIds(Collections.singletonList(response.getScrollId()));
        client.clearScroll(clearScrollRequest);

        return res;

    }

    public List<String> listDictCodes() {
        return Arrays.asList("a", "b", "c");
    }
}
