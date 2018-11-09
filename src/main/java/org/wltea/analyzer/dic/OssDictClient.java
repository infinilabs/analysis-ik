package org.wltea.analyzer.dic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.elasticsearch.common.logging.Loggers;
import org.wltea.analyzer.util.DateHelper;
import org.wltea.analyzer.util.PermissionHelper;

/**
 * @author nick.wn
 * @email nick.wn@alibaba-inc.com
 * @date 2018/11/8
 */
public class OssDictClient {
    private final Logger logger = Loggers.getLogger(OssDictClient.class);

    private OSSClient client;
    private Date stsTokenExpiration;
    private String ECS_METADATA_SERVICE = "http://100.100.100.200/latest/meta-data/ram/security-credentials/";
    private final int IN_TOKEN_EXPIRED_MS = 5000;
    private final int HTTP_STATUS_CODE_OK = 200;
    private final String ACCESS_KEY_ID = "AccessKeyId";
    private final String ACCESS_KEY_SECRET = "AccessKeySecret";
    private final String SECURITY_TOKEN = "SecurityToken";

    private final String ECS_RAM_ROLE_KEY = "ecs_ram_role";
    private final String ENDPOINT_KEY = "oss_endpoint";
    private final String OSS_ACCESS_KEY_ID = "oss_access_key_id";
    private final String OSS_ACCESS_KEY_SECRET = "oss_access_key_secret";
    private final String NOT_SET = "NOT-SET";
    private final String NODE_NAME_FLAG = "node-name-";
    private static CloseableHttpClient httpclient = HttpClients.createDefault();

    private final String EXPIRATION = "Expiration";

    private boolean isStsOssClient;

    public static OssDictClient getInstance() {
        return OssDictClient.LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final OssDictClient INSTANCE = new OssDictClient();
    }

    private OssDictClient() {
        if (StringUtils.isNotEmpty(Dictionary.getSingleton().getProperty(ECS_RAM_ROLE_KEY))) {
            this.isStsOssClient = true;
            this.client = createClient();
        } else {
            this.isStsOssClient = false;
            this.client = createAKOssClient();
        }

    }

    public void shutdown() {
        if (null != this.client) {
            this.client.shutdown();
        }
    }

    private boolean isStsTokenExpired() {
        boolean expired = true;
        Date now = new Date();
        if (null != stsTokenExpiration) {
            if (stsTokenExpiration.after(now)) {
                expired = false;
            }
        }
        return expired;
    }

    private boolean isTokenWillExpired() {
        boolean in = true;
        Date now = new Date();
        long millisecond = stsTokenExpiration.getTime() - now.getTime();
        if (millisecond >= IN_TOKEN_EXPIRED_MS) {
            in = false;
        }
        return in;
    }

    private synchronized OSSClient createClient() {
        if (this.client == null || isStsTokenExpired() || isTokenWillExpired()) {
            shutdown();
            String ecsRamRole = Dictionary.getSingleton().getProperty(ECS_RAM_ROLE_KEY);
            String endpoint = Dictionary.getSingleton().getProperty(ENDPOINT_KEY);
            if (Strings.isBlank(ecsRamRole) || Strings.isBlank(endpoint)) {
                logger.warn(String.format("createClient failed! ecsRamRole or ossEndpoint is null, the ecsRamRole is %s and the ossEndpoint is %s", ecsRamRole, endpoint));
                return null;
            }
            String fullECSMetaDataServiceUrl = ECS_METADATA_SERVICE + ecsRamRole;

            RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000)
                .setConnectTimeout(10 * 1000).setSocketTimeout(15 * 1000).build();
            HttpGet httpGet = new HttpGet(fullECSMetaDataServiceUrl);
            httpGet.setConfig(rc);
            CloseableHttpResponse response = null;
            BufferedReader reader = null;

            try {
                logger.info(String.format("ram role url is %s" , fullECSMetaDataServiceUrl));
                response = httpclient.execute(httpGet);
                if(response.getStatusLine().getStatusCode() == HTTP_STATUS_CODE_OK) {
                    reader = new BufferedReader(new InputStreamReader(
                        response.getEntity().getContent()));
                    String inputLine;
                    StringBuffer responseText = new StringBuffer();
                    while ((inputLine = reader.readLine()) != null) {
                        responseText.append(inputLine);
                    }
                    reader.close();
                    String jsonStringResponse = responseText.toString();
                    logger.info(String.format("response is %s" , jsonStringResponse));
                    JSONObject jsonObjectResponse = JSON.parseObject(jsonStringResponse);
                    String accessKeyId = jsonObjectResponse.getString(ACCESS_KEY_ID);
                    String accessKeySecret = jsonObjectResponse.getString(ACCESS_KEY_SECRET);
                    String securityToken = jsonObjectResponse.getString(SECURITY_TOKEN);
                    this.client = new OSSClient(endpoint, accessKeyId, accessKeySecret, securityToken);
                    stsTokenExpiration = DateHelper.convertStringToDate(jsonObjectResponse.getString(EXPIRATION));
                } else {
                    logger.info(String.format("get oss ramRole %s , return bad code %d" , ecsRamRole, response.getStatusLine().getStatusCode()));
                }

            } catch (Exception e) {
                logger.error("get oss ramRole %s error!", ecsRamRole, e);
            } finally {

                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                try {
                    if (response != null) {
                        response.close();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            return this.client;
        } else {
            return this.client;
        }
    }


    private synchronized OSSClient createAKOssClient() {

        String accessKeyId = Dictionary.getSingleton().getProperty(OSS_ACCESS_KEY_ID);
        String secretAccessKey = Dictionary.getSingleton().getProperty(OSS_ACCESS_KEY_SECRET);
        String endpoint = Dictionary.getSingleton().getProperty(ENDPOINT_KEY);
        if (isStsOssClient || Strings.isBlank(accessKeyId) || Strings.isBlank(secretAccessKey) || Strings.isBlank(endpoint)) {
            return this.client;
        }
        return new OSSClient(endpoint, accessKeyId, secretAccessKey);
    }

    public ObjectMetadata getObjectMetaData(String endpoint) throws OSSException, ClientException, IOException {
        //防止token过期 更新token
        if (isStsOssClient) {
            createClient();
        }
        if (client == null) {
            logger.error(String.format("the oss client is null, maybe is not init!"));
            return null;
        }
        String bucketName = getBucketName(endpoint);
        String prefixKey = getPrefixKey(endpoint);
        if (exists(bucketName, prefixKey)) {
            return PermissionHelper.doPrivileged(() -> this.client.getObjectMetadata(getBucketName(endpoint), getPrefixKey(endpoint)));
        }
        return null;
    }


    public List<String> getObjectContent(String endpoint) throws OSSException, ClientException, IOException {
        //防止token过期 更新token
        if (isStsOssClient) {
            createClient();
        }
        if (client == null) {
            logger.error(String.format("the oss client is null, maybe is not init!"));
            return Collections.emptyList();
        }
        String bucketName = getBucketName(endpoint);
        String prefixKey = getPrefixKey(endpoint);
        logger.info(String.format("the oss bucketName is %s, prefixKey is %s", bucketName, prefixKey));
        if (exists(bucketName, prefixKey)) {
            return convertInputStreamToListString(PermissionHelper.doPrivileged(() -> this.client.getObject(bucketName, prefixKey).getObjectContent()));
        }
        return Collections.emptyList();
    }


    public void updateObjectUserMetaInfo(String endpoint, List<String> otherNodeNameList, String localNodeName, String localNodeETags) throws IOException {
        //防止token过期 更新token
        if (isStsOssClient) {
            createClient();
        }
        if (client == null) {
            logger.error(String.format("the oss client is null, maybe is not init!"));
            return ;
        }
        String bucketName = getBucketName(endpoint);
        String prefixKey = getPrefixKey(endpoint);
        if (exists(bucketName, prefixKey)) {
            CopyObjectRequest request = new CopyObjectRequest(bucketName, prefixKey, bucketName, prefixKey);
            ObjectMetadata meta = PermissionHelper.doPrivileged(() -> this.client.getObjectMetadata(getBucketName(endpoint), getPrefixKey(endpoint)));
            // 设置自定义元信息property值为property-value。
            //本地节点用本地节点的值覆盖，其它节点如果获取的元数据中不存在该节点则用NOT-SET覆盖
            Map<String, String> oldMetaData = meta.getUserMetadata();
            for(String nodeName : otherNodeNameList) {
                if (oldMetaData.get(NODE_NAME_FLAG + nodeName) == null) {
                    meta.addUserMetadata(NODE_NAME_FLAG + nodeName, NOT_SET);
                }
            }
            meta.addUserMetadata(NODE_NAME_FLAG + localNodeName, localNodeETags);
            request.setNewObjectMetadata(meta);
            //修改元信息。
            logger.info("add meta data " + meta.getUserMetadata());
            PermissionHelper.doPrivileged(() -> this.client.copyObject(request));
        }
    }


    private List<String> convertInputStreamToListString(InputStream inputStream) {
        BufferedReader bufferedReader = null;
        try {
            List<String> resultList = new ArrayList<>();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while((line = bufferedReader.readLine()) != null) {
                if (line.trim().startsWith("#")) {
                    continue;
                }
                resultList.add(line);
            }
            return resultList;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }



    private boolean exists(String bucketName, String prefixKey) throws IOException {
        return PermissionHelper.doPrivileged(() -> this.client.doesObjectExist(bucketName, prefixKey));
    }

    private String getBucketName(String endpoint) {
        if (Strings.isBlank(endpoint) || endpoint.length() < 8) {
            return null;
        }
        int bucketNameIndex = endpoint.indexOf("/", 6);
        return endpoint.substring(6, bucketNameIndex);
    }

    private String getPrefixKey(String endpoint) {
        if (Strings.isBlank(endpoint) || endpoint.length() < 8) {
            return null;
        }
        int bucketNameIndex = endpoint.indexOf("/", 6);
        return endpoint.substring(bucketNameIndex + 1);
    }

}
