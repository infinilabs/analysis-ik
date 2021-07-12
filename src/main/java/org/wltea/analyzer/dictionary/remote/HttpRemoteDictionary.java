package org.wltea.analyzer.dictionary.remote;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.dictionary.Dictionary;
import org.wltea.analyzer.dictionary.DictionaryType;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 15:38
 */
class HttpRemoteDictionary extends AbstractRemoteDictionary {

    private static final Logger logger = ESPluginLoggerFactory.getLogger(HttpRemoteDictionary.class.getName());

    private static final CloseableHttpClient httpclient = HttpClients.createDefault();

    private static final Map<String, Modifier> MODIFIER_MAPPING = new ConcurrentHashMap<>();

    @Override
    public Set<String> getRemoteWords(org.wltea.analyzer.dictionary.Dictionary dictionary,
                                      DictionaryType dictionaryType,
                                      URI uri) {
        logger.info("[Remote DictFile Loading] for {}", uri);
        Set<String> words = new HashSet<>();
        RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000).setConnectTimeout(10 * 1000)
                .setSocketTimeout(60 * 1000).build();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;
        BufferedReader in;
        String location = uri.toString();
        HttpGet get = new HttpGet(location);
        get.setConfig(rc);
        try {
            response = httpclient.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {

                String charset = "UTF-8";
                // 获取编码，默认为utf-8
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Header contentType = entity.getContentType();
                    if (contentType != null && contentType.getValue() != null) {
                        String typeValue = contentType.getValue();
                        if (typeValue != null && typeValue.contains("charset=")) {
                            charset = typeValue.substring(typeValue.lastIndexOf("=") + 1);
                        }
                    }

                    if (entity.getContentLength() > 0 || entity.isChunked()) {
                        in = new BufferedReader(new InputStreamReader(entity.getContent(), charset));
                        String line;
                        while ((line = in.readLine()) != null) {
                            words.add(line);
                        }
                        in.close();
                        response.close();
                        return words;
                    }
                }
            }
            response.close();
        } catch (IllegalStateException | IOException e) {
            logger.error("getRemoteWords error {} location {}", e, location);
        }
        return words;
    }

    /**
     * ①向词库服务器发送Head请求
     * ②从响应中获取Last-Modify、ETags字段值，判断是否变化
     * ③如果未变化，休眠1min，返回第①步
     * ④如果有变化，重新加载词典
     * ⑤休眠1min，返回第①步
     */
    @Override
    public void reloadRemoteDictionary(Dictionary dictionary,
                                       DictionaryType dictionaryType,
                                       URI uri) {
        logger.info("[Remote DictFile reloading] for {}", uri);
        //超时设置
        final RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000)
                .setConnectTimeout(10 * 1000).setSocketTimeout(15 * 1000).build();

        String location = uri.toString();
        HttpHead head = new HttpHead(location);
        head.setConfig(rc);
        // 上次更改时间
        String lastModified = null;
        // 资源属性
        String eTags = null;
        Modifier modifier = MODIFIER_MAPPING.get(location);
        if (Objects.nonNull(modifier)) {
            lastModified = modifier.lastModified;
            eTags = modifier.eTags;
        }

        //设置请求头
        if (lastModified != null) {
            head.setHeader("If-Modified-Since", lastModified);
        }
        if (eTags != null) {
            head.setHeader("If-None-Match", eTags);
        }

        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(head);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
                logger.info("[Remote DictFile Reloading] Not modified!");
                return;
            }

            //返回200 才做操作
            if (statusCode == HttpStatus.SC_OK) {
                Header lastHeader = response.getLastHeader("Last-Modified");
                Header eTag = response.getLastHeader("ETag");
                if ((Objects.nonNull(lastHeader) && !lastHeader.getValue().equalsIgnoreCase(lastModified))
                        || (Objects.nonNull(eTag) && !eTag.getValue().equalsIgnoreCase(eTags))) {
                    // 远程词库有更新,需要重新加载词典，并修改last_modified,eTags
                    dictionary.reload(dictionaryType);
                    lastModified = Objects.isNull(lastHeader) ? null : lastHeader.getValue();
                    eTags = Objects.isNull(eTag) ? null : eTag.getValue();
                    MODIFIER_MAPPING.put(location, new Modifier(lastModified, eTags));
                }
                return;
            }
            logger.info("remote_ext_dict {} return bad code {}", location, statusCode);
        } catch (Exception e) {
            logger.error("remote_ext_dict error {} location {} !", e, location);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                logger.error("remote_ext_dict response close error", e);
            }
        }
    }

    @Override
    public String schema() {
        return RemoteDictionarySchema.HTTP.schema;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    static class Modifier {
        /*
         * 上次更改时间
         */
        String lastModified;
        /*
         * 资源属性
         */
        String eTags;
    }
}
