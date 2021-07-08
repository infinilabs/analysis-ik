package org.wltea.analyzer.help;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * RemoteDictDownloader
 *
 * @author Qicz
 * @since 2021/7/8 14:25
 */
public final class RemoteDictDownloader {

	private static final Logger logger = ESPluginLoggerFactory.getLogger(RemoteDictDownloader.class.getName());

	/**
	 * 从远程服务器上下载自定义词条
	 */
	public static List<String> getRemoteWordsUnprivileged(String location) {
		List<String> buffer = new ArrayList<>();
		RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000).setConnectTimeout(10 * 1000)
				.setSocketTimeout(60 * 1000).build();
		CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse response;
		BufferedReader in;
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
							buffer.add(line);
						}
						in.close();
						response.close();
						return buffer;
					}
				}
			}
			response.close();
		} catch (IllegalStateException | IOException e) {
			logger.error("getRemoteWords error {} location {}", e, location);
		}
		return buffer;
	}
}
