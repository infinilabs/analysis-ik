IK Analysis for ElasticSearch
==================================

The IK Analysis plugin integrates Lucene IK analyzer into elasticsearch, support customized dictionary.

In order to install the plugin, simply run: `bin/plugin -install medcl/elasticsearch-analysis-ik/1.0.0`. also download the dict files:


Dict Configuration (es-root/config/ik/IKAnalyzer.cfg.xml)
-------------

<pre>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
	<comment>IK Analyzer 扩展配置</comment>
	<entry key="ext_dict">custom/mydict.dic;custom/sougou.dict</entry>
	<entry key="ext_stopwords">custom/ext_stopword.dic</entry>
</properties>
</pre>

Configuration (elasticsearch.yml)
-------------

<pre>
    {
        "index" : {
            "analysis" : {
                "analyzer" : {
                    "ik" : {
                        type: org.elasticsearch.index.analysis.IkAnalyzerProvider
                    }
                }
            }
        }
    }
</pre>

Mapping Configuration
-------------

Here is a quick example:
1.create a index
<pre>
curl -XPUT http://localhost:9200/index
</pre>

2.create a mapping
<pre>
curl -XPOST http://localhost:9200/index/fulltext/_mapping -d'
{
    "fulltext": {
             "_all": {
            "indexAnalyzer": "ik",
            "searchAnalyzer": "ik",
            "term_vector": "no",
            "store": "false"
        },
        "properties": {
            "content": {
                "type": "string",
                "store": "no",
                "term_vector": "with_positions_offsets",
                "indexAnalyzer": "ik",
                "searchAnalyzer": "ik",
                "include_in_all": "true",
                "boost": 8
            }
        }
    }
}'
</pre>

3.indexing some docs
<pre>
curl -XPOST http://localhost:9200/index/fulltext/1 -d'
{content:"美国留给伊拉克的是个烂摊子吗"}
'

curl -XPOST http://localhost:9200/index/fulltext/2 -d'
{content:"公安部：各地校车将享最高路权"}
'

curl -XPOST http://localhost:9200/index/fulltext/3 -d'
{content:"中韩渔警冲突调查：韩警平均每天扣1艘中国渔船"}
'

curl -XPOST http://localhost:9200/index/fulltext/4 -d'
{content:"中国驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首"}
'
</pre>

4.query with highlighting
<pre>
curl -XPOST http://localhost:9200/index/fulltext/_search  -d'
{
    "query" : { "term" : { "content" : "中国" }},
    "highlight" : {
        "pre_tags" : ["<tag1>", "<tag2>"],
        "post_tags" : ["</tag1>", "</tag2>"],
        "fields" : {
            "content" : {}
        }
    }
}
'
</pre>

here is the query result
<pre>
{
    "took": 14,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "failed": 0
    },
    "hits": {
        "total": 2,
        "max_score": 2,
        "hits": [
            {
                "_index": "index",
                "_type": "fulltext",
                "_id": "4",
                "_score": 2,
                "_source": {
                    "content": "中国驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首"
                },
                "highlight": {
                    "content": [
                        "<tag1>中国</tag1>驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首 "
                    ]
                }
            },
            {
                "_index": "index",
                "_type": "fulltext",
                "_id": "3",
                "_score": 2,
                "_source": {
                    "content": "中韩渔警冲突调查：韩警平均每天扣1艘中国渔船"
                },
                "highlight": {
                    "content": [
                        "均每天扣1艘<tag1>中国</tag1>渔船 "
                    ]
                }
            }
        ]
    }
}
</pre>

have fun.