package org.wltea.analyzer.dic.remote;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;
import org.wltea.analyzer.configuration.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * HttpRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 15:38
 */
public class HttpRemoteDictionary extends AbstractRemoteDictionary implements IRemoteDictionary {

    protected static final Logger logger = ESPluginLoggerFactory.getLogger(HttpRemoteDictionary.class.getName());

    private static final CloseableHttpClient httpclient = HttpClients.createDefault();

    /*
     * 上次更改时间
     */
    private String lastModified;
    /*
     * 资源属性
     */
    private String eTags;

    public HttpRemoteDictionary(Configuration configuration) {
        super(configuration);
    }

    @Override
    public List<String> getRemoteWords(URI uri) {
        List<String> words = new ArrayList<>();
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
    public void reloadRemoteDictionary(URI uri) {
        //超时设置
        RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000)
                .setConnectTimeout(10 * 1000).setSocketTimeout(15 * 1000).build();

        String location = uri.toString();
        HttpHead head = new HttpHead(location);
        head.setConfig(rc);

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

            //返回200 才做操作
            if (response.getStatusLine().getStatusCode() == 200) {

                if (((response.getLastHeader("Last-Modified") != null) && !response.getLastHeader("Last-Modified").getValue().equalsIgnoreCase(lastModified))
                        || ((response.getLastHeader("ETag") != null) && !response.getLastHeader("ETag").getValue().equalsIgnoreCase(eTags))) {

                    // 远程词库有更新,需要重新加载词典，并修改last_modified,eTags
                    Dictionary.getDictionary().reloadMainDict();
                    lastModified = response.getLastHeader("Last-Modified") == null ? null : response.getLastHeader("Last-Modified").getValue();
                    eTags = response.getLastHeader("ETag") == null ? null : response.getLastHeader("ETag").getValue();
                }
            } else if (response.getStatusLine().getStatusCode() == 304) {
                //没有修改，不做操作
                //noop
            } else {
                logger.info("remote_ext_dict {} return bad code {}", location, response.getStatusLine().getStatusCode());
            }

        } catch (Exception e) {
            logger.error("remote_ext_dict error {} location {} !", e, location);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
