package org.wltea.analyzer.dic;


import java.io.IOException;
import java.util.Date;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.ESLoggerFactory;

public class OSSMonitor implements Runnable {

	private static final Logger logger = ESLoggerFactory.getLogger(OSSMonitor.class.getName());

	/**
	 * 上次更改时间
	 */
	private Date last_modified;
	/**
	 * 资源属性
	 */
	private String eTags;

	/**
	 *
	 */
	private String endpoint;


	public OSSMonitor(String endpoint) {
		this.endpoint = endpoint;
		this.last_modified = null;
		this.eTags = null;
	}
	/**
	 * 监控流程：
	 *  ①从响应中获取Last-Modify、ETags字段值，判断是否变化
	 *  ②如果未变化，休眠1min，返回第①步
	 *  ③如果有变化，重新加载词典
	 * 	④休眠1min，返回第①步
	 */

	@Override
	public void run() {
		OssDictClient ossDictClient = OssDictClient.getInstance();
		try {
			ObjectMetadata objectMetadata = ossDictClient.getObjectMetaData(this.endpoint);
			if (objectMetadata != null
				&& (!objectMetadata.getETag().equalsIgnoreCase(eTags) || !objectMetadata.getLastModified().equals(last_modified))) {
				eTags = objectMetadata.getETag();
				last_modified = objectMetadata.getLastModified();
				//reload dict
				// 远程词库有更新,需要重新加载词典，并修改last_modified,eTags
				Dictionary.getSingleton().reLoadMainDict();
			}
		} catch (OSSException e) {
			if (!e.getErrorCode().equals("404")) {
				logger.error("get dict from oss failed!", e);
			}
		} catch (ClientException | IOException e) {
			logger.error("oss client exception !", e);
		}
	}

}
