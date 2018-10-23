package org.wltea.analyzer.dic;


import java.io.IOException;
import java.util.Date;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;

public class OSSMonitor implements Runnable {

	private static final Logger logger = ESLoggerFactory.getLogger(OSSMonitor.class.getName());

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
			if (objectMetadata != null && !objectMetadata.getETag().equalsIgnoreCase(eTags)) {
				//reload dict
				// 远程词库有更新,需要重新加载词典，并修改last_modified,eTags
				Dictionary.getSingleton().reLoadMainDict();
				eTags = objectMetadata.getETag();
				logger.info(String.format("endpoint is %s, etags is %s", this.endpoint, eTags));
			}
			if (objectMetadata != null && Strings.isNotBlank(eTags) && AnalysisIkPlugin.clusterService.state().nodes().getLocalNode() != null) {
                String nodeName = AnalysisIkPlugin.clusterService.localNode().getName();
			    if (objectMetadata.getUserMetadata() == null || objectMetadata.getUserMetadata().get(nodeName.toLowerCase()) == null
                        || !objectMetadata.getUserMetadata().get(nodeName.toLowerCase()).equals(eTags)) {
                        logger.info(String.format("node name is %s and will upload etags to oss file! The eTags is %s", nodeName, eTags));
                        ossDictClient.updateObjectUserMetaInfo(this.endpoint, nodeName.toLowerCase(), eTags);
                }
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
