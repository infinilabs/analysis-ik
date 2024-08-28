IK Analysis for Elasticsearch and OpenSearch
==================================

![](./assets/banner.png)

The IK Analysis plugin integrates Lucene IK analyzer, and support customized dictionary.  It supports major versions of Elasticsearch and OpenSearch. Maintained and supported with ❤️ by [INFINI Labs](https://infinilabs.com).

The plugin comprises analyzer: `ik_smart` , `ik_max_word`, and tokenizer: `ik_smart` , `ik_max_word`

# How to Install

You can download the packaged plugins from here: `https://release.infinilabs.com/`, 

or you can use the `plugin` cli to install the plugin like this:

For Elasticsearch

```
bin/elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/8.4.1
```

For OpenSearch

```
bin/opensearch-plugin install https://get.infini.cloud/opensearch/analysis-ik/2.12.0
```

Tips: replace your own version number related to your elasticsearch or opensearch.


# Getting Started

1.create a index

```bash
curl -XPUT http://localhost:9200/index
```

2.create a mapping

```bash
curl -XPOST http://localhost:9200/index/_mapping -H 'Content-Type:application/json' -d'
{
        "properties": {
            "content": {
                "type": "text",
                "analyzer": "ik_max_word",
                "search_analyzer": "ik_smart"
            }
        }

}'
```

3.index some docs

```bash
curl -XPOST http://localhost:9200/index/_create/1 -H 'Content-Type:application/json' -d'
{"content":"美国留给伊拉克的是个烂摊子吗"}
'
```

```bash
curl -XPOST http://localhost:9200/index/_create/2 -H 'Content-Type:application/json' -d'
{"content":"公安部：各地校车将享最高路权"}
'
```

```bash
curl -XPOST http://localhost:9200/index/_create/3 -H 'Content-Type:application/json' -d'
{"content":"中韩渔警冲突调查：韩警平均每天扣1艘中国渔船"}
'
```

```bash
curl -XPOST http://localhost:9200/index/_create/4 -H 'Content-Type:application/json' -d'
{"content":"中国驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首"}
'
```

4.query with highlighting

```bash
curl -XPOST http://localhost:9200/index/_search  -H 'Content-Type:application/json' -d'
{
    "query" : { "match" : { "content" : "中国" }},
    "highlight" : {
        "pre_tags" : ["<tag1>", "<tag2>"],
        "post_tags" : ["</tag1>", "</tag2>"],
        "fields" : {
            "content" : {}
        }
    }
}
'
```

Result

```json
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
```

针对ES 的 match_phrase query 搜索，是一个非常消耗CPU的query，因为需要处理 term 和 position 的相对位置。为了加速搜素，现优化了分词形式，保存了正确position 的相对位置信息，使得match_phrase query 可以在分词条件下使用，经测试使用该分词之后查询降为原来的 10% 以下。该分词器分为 index 和 search分词器，分别用于索引数据和查询数据。</br>
原理是分词出来的词项对应着首字 position ，所以可以在倒排中保存相对位置信息。index 分词器是切分出了所有的组合，search 分词器是没有重复的切出最少词项的组合，且不会重复。</br>
使用：</br>
1, 定义text 字段，analyzer 设置为 index 分词器，search_analyer 设置为 search分词器；</br>
2, 写数据。</br>
3, 查询。</br>
4, 分词器首字确定位置： fcp_index、fcp_search; 末字确定位置：lcp_index、lcp_search</br>
5, 缺点是目前原生的高亮不支持这种分词方式</br>

原理<br>

```json
# 使用index 分词是，最细粒度的，按照字的position确定词的position，确定了position的取值标准
POST /_analyze
{
  "analyzer": "fcp_index",
  "text": "中国平安"
}
# response
{
  "tokens": [
    {
      "token": "中",
      "start_offset": 0,
      "end_offset": 0,
      "type": "<CHAR_CHINESE>",
      "position": 0
    },
    {
      "token": "中国",
      "start_offset": 0,
      "end_offset": 0,
      "type": "<COMBINE_WORD>",
      "position": 0
    },
    {
      "token": "国",
      "start_offset": 0,
      "end_offset": 0,
      "type": "<CHAR_CHINESE>",
      "position": 1
    },
    {
      "token": "平",
      "start_offset": 0,
      "end_offset": 0,
      "type": "<CHAR_CHINESE>",
      "position": 2
    },
    {
      "token": "平安",
      "start_offset": 0,
      "end_offset": 0,
      "type": "<COMBINE_WORD>",
      "position": 2
    },
    {
      "token": "安",
      "start_offset": 0,
      "end_offset": 0,
      "type": "<CHAR_CHINESE>",
      "position": 3
    }
  ]
}
# 使用search 分词是粗粒度、无重叠分词，但仍按照字的position确定词的position，所以使用match_phrase有效
POST /_analyze
{
  "analyzer": "fcp_search",
  "text": "中国平安"
}
# response
{
  "tokens": [
    {
      "token": "中国",
      "start_offset": 0,
      "end_offset": 2,
      "type": "<COMBINE_WORD>",
      "position": 0
    },
    {
      "token": "平安",
      "start_offset": 2,
      "end_offset": 4,
      "type": "<COMBINE_WORD>",
      "position": 2
    }
  ]
}
```

```json
PUT test_index
{
  "mappings": {
    "properties": {
      "content":{
        "type": "text",
        "analyzer": "fcp_index",
        "search_analyzer": "fcp_search"
      }
    }
  }
}

POST test_index/_doc/1
{
  "content": "如果需要覆盖原来的配置"
}

GET test_index/_search
{
  "query": {
    "match_phrase": {
      "content": {
        "query": "要覆盖"
      }
    }
  }
}
```

# Dictionary Configuration

Config file `IKAnalyzer.cfg.xml` can be located at `{conf}/analysis-ik/config/IKAnalyzer.cfg.xml`
or `{plugins}/elasticsearch-analysis-ik-*/config/IKAnalyzer.cfg.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
	<entry key="ext_dict">custom/mydict.dic;custom/single_word_low_freq.dic</entry>
	<entry key="ext_stopwords">custom/ext_stopword.dic</entry>
	<entry key="remote_ext_dict">location</entry>
	<entry key="remote_ext_stopwords">http://xxx.com/xxx.dic</entry>
</properties>
```

## Hot-reload Dictionary

The current plugin supports hot reloading dictionary for IK Analysis, through the configuration mentioned earlier in the IK configuration file.

```xml
	<entry key="remote_ext_dict">location</entry>
	<entry key="remote_ext_stopwords">location</entry>
```

Among which `location` refers to a URL, such as `http://yoursite.com/getCustomDict`. This request only needs to meet the following two points to complete the segmentation hot update.

1. The HTTP request needs to return two headers, one is `Last-Modified`, and the other is `ETag`. Both of these are of string type, and if either changes, the plugin will fetch new segmentation to update the word library.

2. The content format returned by the HTTP request is one word per line, and the newline character is represented by `\n`.

Meeting the above two requirements can achieve hot word updates without the need to restart the ES instance.

You can place the hot words that need to be automatically updated in a .txt file encoded in UTF-8. Place it under nginx or another simple HTTP server. When the .txt file is modified, the HTTP server will automatically return the corresponding Last-Modified and ETag when the client requests the file. You can also create a separate tool to extract relevant vocabulary from the business system and update this .txt file.

## FAQs
-------

1. Why isn't the custom dictionary taking effect?

Please ensure that the text format of your custom dictionary is UTF8 encoded.

2. What is the difference between ik_max_word and ik_smart?

ik_max_word: Performs the finest-grained segmentation of the text. For example, it will segment "中华人民共和国国歌" into "中华人民共和国,中华人民,中华,华人,人民共和国,人民,人,民,共和国,共和,和,国国,国歌", exhaustively generating various possible combinations, suitable for Term Query.

ik_smart: Performs the coarsest-grained segmentation of the text. For example, it will segment "中华人民共和国国歌" into "中华人民共和国,国歌", suitable for Phrase queries.

Note: ik_smart is not a subset of ik_max_word.

# Community

Fell free to join the Discord server to discuss anything around this project: 

[https://discord.gg/4tKTMkkvVX](https://discord.gg/4tKTMkkvVX)

# License

Copyright ©️ INFINI Labs.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
