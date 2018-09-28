package org.wltea.analyzer.dic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import static java.lang.Thread.sleep;


public class OssDictClient {
    private final Logger logger = Loggers.getLogger(OssDictClient.class);

    private OSSClient client;
    private Date stsTokenExpiration;
    private String ECS_METADATA_SERVICE = "http://100.100.100.200/latest/meta-data/ram/security-credentials/";
    private final int IN_TOKEN_EXPIRED_MS = 5000;
    private final String ACCESS_KEY_ID = "AccessKeyId";
    private final String ACCESS_KEY_SECRET = "AccessKeySecret";
    private final String SECURITY_TOKEN = "SecurityToken";
    private final int REFRESH_RETRY_COUNT = 3;
    private boolean isStsOssClient;
    private ReadWriteLock readWriteLock;

    private final String ECS_RAM_ROLE_KEY = "ecsRamRole";
    private final String ENDPOINT_KEY = "endpoint";
    private static CloseableHttpClient httpclient = HttpClients.createDefault();

    private final String EXPIRATION = "Expiration";


    public static OssDictClient getInstance() {
        return OssDictClient.LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final OssDictClient INSTANCE = new OssDictClient();
    }

    private OssDictClient() {
        this.isStsOssClient = true;
        this.readWriteLock = new ReentrantReadWriteLock();
        try {
            this.client = createClient();
        } catch (ClientCreateException e) {
            logger.error("create oss client failed!", e);
        }
    }

    public boolean isStsOssClient() {
        return isStsOssClient;
    }

    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest)
            throws OSSException, ClientException {
        if (isStsOssClient) {
            readWriteLock.readLock().lock();
            try {
                return this.client.deleteObjects(deleteObjectsRequest);
            } finally {
                readWriteLock.readLock().unlock();
            }
        } else {
            return this.client.deleteObjects(deleteObjectsRequest);
        }
    }


    public boolean doesObjectExist(String bucketName, String key)
            throws OSSException, ClientException {
        if (isStsOssClient) {
            readWriteLock.readLock().lock();
            try {
                return this.client.doesObjectExist(bucketName, key);
            } finally {
                readWriteLock.readLock().unlock();
            }
        } else {
            return this.client.doesObjectExist(bucketName, key);
        }
    }

    public boolean doesBucketExist(String bucketName)
            throws OSSException, ClientException {
        if (isStsOssClient) {
            readWriteLock.readLock().lock();
            try {
                return this.client.doesBucketExist(bucketName);
            } finally {
                readWriteLock.readLock().unlock();
            }
        } else {
            return this.client.doesBucketExist(bucketName);
        }
    }


    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest)
            throws OSSException, ClientException {
        if (isStsOssClient) {
            readWriteLock.readLock().lock();
            try {
                return this.client.listObjects(listObjectsRequest);
            } finally {
                readWriteLock.readLock().unlock();
            }
        } else {
            return this.client.listObjects(listObjectsRequest);
        }
    }


    public OSSObject getObject(String bucketName, String key)
            throws OSSException, ClientException {
        if (isStsOssClient) {
            readWriteLock.readLock().lock();
            try {
                return this.client.getObject(bucketName, key);
            } finally {
                readWriteLock.readLock().unlock();
            }
        } else {
            return this.client.getObject(bucketName, key);
        }
    }


    public PutObjectResult putObject(String bucketName, String key, InputStream input,
                                     ObjectMetadata metadata) throws OSSException, ClientException {
        if (isStsOssClient) {
            readWriteLock.readLock().lock();
            try {
                return this.client.putObject(bucketName, key, input, metadata);
            } finally {
                readWriteLock.readLock().unlock();
            }
        } else {
            return this.client.putObject(bucketName, key, input, metadata);
        }
    }


    public void deleteObject(String bucketName, String key)
            throws OSSException, ClientException {
        if (isStsOssClient) {
            readWriteLock.readLock().lock();
            try {
                this.client.deleteObject(bucketName, key);
            } finally {
                readWriteLock.readLock().unlock();
            }
        } else {
            this.client.deleteObject(bucketName, key);
        }

    }


    public CopyObjectResult copyObject(String sourceBucketName, String sourceKey,
                                       String destinationBucketName, String destinationKey) throws OSSException, ClientException {

        if (isStsOssClient) {
            readWriteLock.readLock().lock();
            try {
                return this.client
                        .copyObject(sourceBucketName, sourceKey, destinationBucketName, destinationKey);
            } finally {
                readWriteLock.readLock().unlock();
            }
        } else {
            return this.client
                    .copyObject(sourceBucketName, sourceKey, destinationBucketName, destinationKey);
        }
    }

    public void refreshStsOssClient() throws ClientCreateException {
        int retryCount = 0;
        while (isStsTokenExpired() || isTokenWillExpired()) {
            retryCount++;
            if (retryCount > REFRESH_RETRY_COUNT) {
                logger.error("Can't get valid token after retry {} times", REFRESH_RETRY_COUNT);
                throw new ClientCreateException("Can't get valid token after retry " + REFRESH_RETRY_COUNT + " times");
            }
            this.client = createStsOssClient();
            try {
                if (isStsTokenExpired() || isTokenWillExpired()) {
                    sleep(IN_TOKEN_EXPIRED_MS * 2);
                }
            } catch (InterruptedException e) {
                logger.error("refresh sleep exception", e);
                throw new ClientCreateException(e);
            }
        }
    }

    public void shutdown() {
        if (isStsOssClient) {
            readWriteLock.writeLock().lock();
            try {
                if (null != this.client) {
                    this.client.shutdown();
                }
            } finally {
                readWriteLock.writeLock().unlock();
            }
        } else {
            if (null != this.client) {
                this.client.shutdown();
            }
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

    private OSSClient createClient() throws ClientCreateException {
        return createStsOssClient();
    }


    private synchronized OSSClient createStsOssClient() throws ClientCreateException {
        if (isStsTokenExpired() || isTokenWillExpired()) {

            String ecsRamRole = Dictionary.getSingleton().getProperty(ECS_RAM_ROLE_KEY);
            String endpoint = Dictionary.getSingleton().getProperty(ENDPOINT_KEY);

            String fullECSMetaDataServiceUrl = ECS_METADATA_SERVICE + ecsRamRole;
            RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10*1000)
                .setConnectTimeout(10*1000).setSocketTimeout(15*1000).build();
            HttpHead head = new HttpHead(fullECSMetaDataServiceUrl);
            head.setConfig(rc);
            CloseableHttpResponse response = null;

            try {
                response = httpclient.execute(head);
                if(response.getStatusLine().getStatusCode() == 200) {
                    String jsonStringResponse = response.getEntity().getContentType().getValue();
                    JSONObject jsonObjectResponse = JSON.parseObject(jsonStringResponse);
                    String accessKeyId = jsonObjectResponse.getString(ACCESS_KEY_ID);
                    String accessKeySecret = jsonObjectResponse.getString(ACCESS_KEY_SECRET);
                    String securityToken = jsonObjectResponse.getString(SECURITY_TOKEN);
                    this.client = new OSSClient(endpoint, accessKeyId, accessKeySecret, securityToken);
                } else {
                    logger.info(String.format("get oss ramRole %s , return bad code %d" , ecsRamRole, response.getStatusLine().getStatusCode()));
                }

            } catch (Exception e) {
                logger.error("get oss ramRole %s error!", ecsRamRole, e);
            } finally {
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


    //TODO
    public OSSObject getDictsObject()
        throws OSSException, ClientException {
        if (isStsOssClient) {
            readWriteLock.readLock().lock();
            try {
                return this.client.getObject("", "");
            } finally {
                readWriteLock.readLock().unlock();
            }
        } else {
            return this.client.getObject("", "");
        }
    }
}
