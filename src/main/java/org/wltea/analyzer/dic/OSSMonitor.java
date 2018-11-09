package org.wltea.analyzer.dic;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;


/**
 * @author nick.wn
 * @email nick.wn@alibaba-inc.com
 * @date 2018/11/8
 */

public class OSSMonitor implements Runnable {

	private static final Logger logger = ESLoggerFactory.getLogger(OSSMonitor.class.getName());
	private final String NODE_NAME_FLAG = "node-name-";

	/**
	 * 资源属性
	 */
	private String eTags;

	/**
	 * 请求OSS地址
	 */
	private String endpoint;

	/**
	 * 词典类型
	 */
	private DictType dictType;



	public OSSMonitor(String endpoint, DictType dictType) {
		this.endpoint = endpoint;
		this.eTags = null;
		this.dictType = dictType;
	}
	/**
	 * 监控流程：
	 *  ①从响应中获取Last-Modify、ETags字段值，判断是否变化
	 *  ②如果未变化，休眠1min，返回第①步
	 *  ③如果有变化，重新加载词典
	 * 	④休眠1min，返回第①步
	 * 	当节点更新oss词典完毕后 回写该节点的nodeName及当前的etags信息到oss文件的用户自定义元数据信息中
	 * 	（如果有需要 业务层可以根据获取该oss文件的元数据信息知道哪些节点更新完毕词典）
	 */

	@Override
	public void run() {
		OssDictClient ossDictClient = OssDictClient.getInstance();
		try {
			ObjectMetadata objectMetadata = ossDictClient.getObjectMetaData(this.endpoint);
			if (objectMetadata != null && !objectMetadata.getETag().equalsIgnoreCase(eTags)) {
				//reload dict
				// 远程词库有更新,需要重新加载词典，并修改last_modified,eTags
				if (dictType.equals(DictType.MAIN)) {
					Dictionary.getSingleton().reLoadMainDicts();
				} else {
					Dictionary.getSingleton().reLoadStopWordDict();
				}
				eTags = objectMetadata.getETag();
			}
			if (objectMetadata != null && Strings.isNotBlank(eTags) && AnalysisIkPlugin.clusterService.state().nodes().getLocalNode() != null) {
                String localNodeName = AnalysisIkPlugin.clusterService.localNode().getName().toLowerCase();
                String ossLocalNodeName = NODE_NAME_FLAG + localNodeName;
				if (objectMetadata.getUserMetadata() == null || objectMetadata.getUserMetadata().get(ossLocalNodeName) == null
                        || !objectMetadata.getUserMetadata().get(ossLocalNodeName).equals(eTags)) {
                        List<String> otherNodeNameList = new ArrayList<>();
                        for (DiscoveryNode node : AnalysisIkPlugin.clusterService.state().nodes()) {
							if (!(NODE_NAME_FLAG + node.getName().toLowerCase()).equals(ossLocalNodeName)) {
								otherNodeNameList.add(NODE_NAME_FLAG + node.getName().toLowerCase());
							}
						}
						AnalysisIkPlugin.clusterService.state().getNodes();
						ossDictClient.updateObjectUserMetaInfo(this.endpoint, otherNodeNameList, ossLocalNodeName, eTags);
			    }
            }
		} catch (OSSException e) {
			if (!e.getErrorCode().equals("404")) {
				logger.error("get dict from oss failed or update file meta data failed!", e);
			}
		} catch (ClientException | IOException e) {
			logger.error("oss client exception !", e);
		}
	}

}
