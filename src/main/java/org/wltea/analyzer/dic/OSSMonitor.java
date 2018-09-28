package org.wltea.analyzer.dic;


import com.aliyun.oss.model.OSSObject;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.ESLoggerFactory;

public class OSSMonitor implements Runnable {

	private static final Logger logger = ESLoggerFactory.getLogger(OSSMonitor.class.getName());

	/**
	 * 上次更改时间
	 */
	private String last_modified;
	/**
	 * 资源属性
	 */
	private String eTags;


	public OSSMonitor() {
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
	    //TODO 需要判断词典文件是否存在
	    OSSObject ossObject = ossDictClient.getDictsObject();

	}

}
