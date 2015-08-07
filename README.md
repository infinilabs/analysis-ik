IK Analysis for ElasticSearch
=============================

The IK Analysis plugin integrates Lucene IK analyzer into elasticsearch, support customized dictionary.

Tokenizer: `ik`

更新：对于使用 ES 集群，用 IK 作为分词插件，经常会修改自定义词典的使用者，可以透过远程加载的方式，每次更新都会重新加载词典，不必重启 ES 服务。

Versions
--------

IK version | ES version
-----------|-----------
master | 1.5.0 -> master
1.4.0 | 1.6.0
1.3.0 | 1.5.0
1.2.9 | 1.4.0
1.2.8 | 1.3.2
1.2.7 | 1.2.1
1.2.6 | 1.0.0
1.2.5 | 0.90.2
1.2.3 | 0.90.2
1.2.0 | 0.90.0
1.1.3 | 0.20.2
1.1.2 | 0.19.x
1.0.0 | 0.16.2 -> 0.19.0

Install
-------

you can download this plugin from RTF project(https://github.com/medcl/elasticsearch-rtf)
https://github.com/medcl/elasticsearch-rtf/tree/master/plugins/analysis-ik
https://github.com/medcl/elasticsearch-rtf/tree/master/config/ik

<del>also remember to download the dict files,unzip these dict file into your elasticsearch's config folder,such as: your-es-root/config/ik</del>

you need a service restart after that!

Configuration
-------------

### Analysis Configuration

#### `elasticsearch.yml`

```yaml
index:
  analysis:
    analyzer:
      ik:
          alias: [ik_analyzer]
          type: org.elasticsearch.index.analysis.IkAnalyzerProvider
      ik_max_word:
          type: ik
          use_smart: false
      ik_smart:
          type: ik
          use_smart: true
```

Or

```yaml
index.analysis.analyzer.ik.type : "ik"
```

#### 以上两种配置方式的区别：

1、第二种方式，只定义了一个名为 ik 的 analyzer，其 use_smart 采用默认值 false

2、第一种方式，定义了三个 analyzer，分别为：ik、ik_max_word、ik_smart，其中 ik_max_word 和 ik_smart 是基于 ik 这个 analyzer 定义的，并各自明确设置了 use_smart 的不同值。

3、其实，ik_max_word 等同于 ik。ik_max_word 会将文本做最细粒度的拆分，比如会将“中华人民共和国国歌”拆分为“中华人民共和国,中华人民,中华,华人,人民共和国,人民,人,民,共和国,共和,和,国国,国歌”，会穷尽各种可能的组合；而 ik_smart 会做最粗粒度的拆分，比如会将“中华人民共和国国歌”拆分为“中华人民共和国,国歌”。

因此，建议，在设置 mapping 时，用 ik 这个 analyzer，以尽可能地被搜索条件匹配到。

不过，如果你想将 /index_name/_analyze 这个 RESTful API 做为分词器用，用来提取某段文字中的主题词，则建议使用 ik_smart 这个 analyzer：

```
POST /hailiang/_analyze?analyzer=ik_smart HTTP/1.1
Host: localhost:9200
Cache-Control: no-cache

中华人民共和国国歌
```

返回值：

```json
{
  "tokens" : [ {
    "token" : "中华人民共和国",
    "start_offset" : 0,
    "end_offset" : 7,
    "type" : "CN_WORD",
    "position" : 1
  }, {
    "token" : "国歌",
    "start_offset" : 7,
    "end_offset" : 9,
    "type" : "CN_WORD",
    "position" : 2
  } ]
}
```

另外，可以在 elasticsearch.yml 里加上如下一行，设置默认的 analyzer 为 ik：

```yaml
index.analysis.analyzer.default.type : "ik"
```


### Mapping Configuration

#### Quick Example

1. create a index

```bash
curl -XPUT http://localhost:9200/index
```

2. create a mapping

```bash
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
```

3. index some docs

```bash
curl -XPOST http://localhost:9200/index/fulltext/1 -d'
{"content":"美国留给伊拉克的是个烂摊子吗"}
'
```

```bash
curl -XPOST http://localhost:9200/index/fulltext/2 -d'
{"content":"公安部：各地校车将享最高路权"}
'
```

```bash
curl -XPOST http://localhost:9200/index/fulltext/3 -d'
{"content":"中韩渔警冲突调查：韩警平均每天扣1艘中国渔船"}
'
```

```bash
curl -XPOST http://localhost:9200/index/fulltext/4 -d'
{"content":"中国驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首"}
'
```

4. query with highlighting

```bash
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
```

#### Result

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

### Dictionary Configuration

#### `config/ik/IKAnalyzer.cfg.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
	<comment>IK Analyzer 扩展配置</comment>
	<!--用户可以在这里配置自己的扩展字典 -->
	<entry key="ext_dict">custom/mydict.dic;custom/single_word_low_freq.dic</entry>
	 <!--用户可以在这里配置自己的扩展停止词字典-->
	<entry key="ext_stopwords">custom/ext_stopword.dic</entry>
 	<!--用户可以在这里配置远程扩展字典 -->
	<entry key="remote_ext_dict">location</entry>
 	<!--用户可以在这里配置远程扩展停止词字典-->
	<entry key="remote_ext_stopwords">location</entry>
</properties>
```

### 热更新 IK 分词使用方法

目前该插件支持热更新 IK 分词，通过上文在 IK 配置文件中提到的如下配置

```xml
 	<!--用户可以在这里配置远程扩展字典 -->
	<entry key="remote_ext_dict">location</entry>
 	<!--用户可以在这里配置远程扩展停止词字典-->
	<entry key="remote_ext_stopwords">location</entry>
```

其中 `location` 是指一个 url，比如 `http://yoursite.com/getCustomDict`，该请求只需满足以下两点即可完成分词热更新。

1. 该 http 请求需要返回两个头部(header)，一个是 `Last-Modified`，一个是 `ETags`，这两者都是字符串类型，只要有一个发生变化，该插件就会去抓取新的分词进而更新词库。

2. 该 http 请求返回的内容格式是一行一个分词，换行符用 `\n` 即可。

满足上面两点要求就可以实现热更新分词了，不需要重启 ES 实例。

have fun.

常见问题
-------

1.自定义词典为什么没有生效？

请确保你的扩展词典的文本格式为 UTF8 编码

2.如何手动安装，以 1.3.0 為例？（参考：https://github.com/medcl/elasticsearch-analysis-ik/issues/46）


```bash
git clone https://github.com/medcl/elasticsearch-analysis-ik
cd elasticsearch-analysis-ik
mvn compile
mvn package
plugin --install analysis-ik --url file:///#{project_path}/elasticsearch-analysis-ik/target/releases/elasticsearch-analysis-ik-1.3.0.zip
```

Thanks
------
YourKit supports IK Analysis for ElasticSearch project with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
<a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
<a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.
