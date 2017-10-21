/**
* Copyright 2017 (C) , All Rights Reserved.
* Company: 深圳联友科技有限公司.
*
* Create At 2017年9月12日.
*
*/
package com.swift.common.util;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;

import javax.net.ssl.SSLException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;


/** 
 * @ClassName: HttpClientUtils 
 * @Description: 
 * @date 2017年9月12日 上午11:32:27 
 *  
 */
public class HttpClientUtils {

	private static final Logger logger = Logger.getLogger(HttpClientUtils.class);
	
	private static final int MANANGER_POOL_CONNECTION_TIMEOUT = 2; //从连接池获取连接超时时间2秒钟
	private static final int SO_TIMEOUT  = 3; //默认超时时间3秒钟
	private static final int RETRY_COUNT = 2; //默认重试次数2次
	private static final int DEFAULT_MAX_PER_ROUTE_COUNT = 3;
	private static final int MAX_TOTAL_COUNT = 3;
	private static final int HTTP_DEFAULT_KEEP_TIME = 1; 
	private static PoolingHttpClientConnectionManager connManager = null;
	private static CloseableHttpClient  httpClient = null;
	 /**
     * 默认content 类型
     */
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
	
    private static HttpClient client = null;

	static {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(128);
		cm.setDefaultMaxPerRoute(128);
		client = HttpClients.custom().setConnectionManager(cm).build();
	}
    /**
     * Http connection keepAlive 设置
     */
    public static ConnectionKeepAliveStrategy kaStrategy = new ConnectionKeepAliveStrategy() {
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            int keepTime = HTTP_DEFAULT_KEEP_TIME;
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    try {
                        return Long.parseLong(value) * 1000;
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("format KeepAlive timeout exception, exception:" + e.toString());
                    }
                }
            }
            return keepTime * 1000;
        }
    };
    
    /**
     * 初始化连接池
     */
    private static synchronized void initPools() {
        if (httpClient == null) {
        	connManager = new PoolingHttpClientConnectionManager();
        	connManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE_COUNT);
        	connManager.setMaxTotal(MAX_TOTAL_COUNT); 
        	httpClient = HttpClients.custom().setKeepAliveStrategy(kaStrategy).setRetryHandler(httpRequestRetryHandler).setConnectionManager(connManager).build();
        }
    }
	
    
	/**
	 * 重试策略	
	 */
    private static HttpRequestRetryHandler  httpRequestRetryHandler = new HttpRequestRetryHandler() { 
    		@Override
			public boolean retryRequest(IOException exception,
					int executionCount, HttpContext context) {
				logger.info("重试次数："+ executionCount);
				if (executionCount > RETRY_COUNT) {
					return false;
				}
				if (exception instanceof ConnectTimeoutException) {
					logger.error("error:连接超时"); 
					return true;
				}
				if (exception instanceof InterruptedIOException) {
					logger.error("error:IO异常"); 
					return true;
				}
				if (exception instanceof ConnectException) {
					return true;
				}
				if (exception instanceof SSLException) {
					return false;
				}
				return false;
			}
			
    };

	
	 public static CloseableHttpClient getHttpClient() {
	        return httpClient;
	 }


   
    /**
     * 创建请求
     *
     * @param uri 请求url
     * @param methodName 请求的方法类型
     * @param contentType contentType类型
     * @param timeout 超时时间
     * @return
     */
    private static HttpRequestBase getRequest(String uri, String methodName, String contentType, int timeout) {
        if (httpClient == null) {
            initPools();
        }
        HttpRequestBase method = null;
        if (timeout <= 0) {
            timeout = SO_TIMEOUT;
        }
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(MANANGER_POOL_CONNECTION_TIMEOUT * 1000) // 从连接池获取连接的超时时间
				.setConnectTimeout(timeout * 1000) // 连接的超时时间(三次握手时间)
				.setSocketTimeout(timeout * 1000) // socket读写超时(通信阶段)
				.setExpectContinueEnabled(false).build();

        if (HttpPut.METHOD_NAME.equalsIgnoreCase(methodName)) {
            method = new HttpPut(uri);
        } else if (HttpPost.METHOD_NAME.equalsIgnoreCase(methodName)) {
            method = new HttpPost(uri);
        } else if (HttpGet.METHOD_NAME.equalsIgnoreCase(methodName)) {
            method = new HttpGet(uri);
        } else {
            method = new HttpPost(uri);
        }
        if (StringUtils.isBlank(contentType)) {
            contentType = DEFAULT_CONTENT_TYPE;
        }
        method.addHeader("Content-Type", contentType);
        method.addHeader("Accept", contentType);
        method.setConfig(requestConfig);
        return method;
    }

    /**
     * 执行GET 请求
     *
     * @param uri
     * @return
     */
    public static String executeGet(String uri) {
        long startTime = System.currentTimeMillis();
        HttpEntity httpEntity = null;
        HttpRequestBase method = null;
        String responseBody = "";
        try {
            if (httpClient == null) {
                initPools();
            }
            method = getRequest(uri, HttpPost.METHOD_NAME, DEFAULT_CONTENT_TYPE, 0);
            HttpContext context = HttpClientContext.create();
            CloseableHttpResponse httpResponse = httpClient.execute(method, context);
            httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                responseBody = EntityUtils.toString(httpEntity, "UTF-8");
                logger.info("请求URL: "+uri+"+  返回状态码："+httpResponse.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            if(method != null){
                method.abort();
            }
            e.printStackTrace();
            logger.error("execute get request exception, url:" + uri + ", exception:" + e.toString() + ",cost time(ms):"
                    + (System.currentTimeMillis() - startTime));
        } finally {
            if (httpEntity != null) {
                try {
                    EntityUtils.consumeQuietly(httpEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("close response exception, url:" + uri + ", exception:" + e.toString() + ",cost time(ms):"
                            + (System.currentTimeMillis() - startTime));
                }
            }
        }
        return responseBody;
    }
	
    /**
     * 执行http post请求 默认采用Content-Type：application/json，Accept：application/json
     *
     * @param uri 请求地址
     * @param data  请求数据
     * @return
     */
    public static String executePost(String uri) {
        long startTime = System.currentTimeMillis();
        HttpEntity httpEntity = null;
        HttpEntityEnclosingRequestBase method = null;
        String responseBody = "";
        try {
            if (httpClient == null) {
                initPools();
            }
            method = (HttpEntityEnclosingRequestBase) getRequest(uri, HttpPost.METHOD_NAME, DEFAULT_CONTENT_TYPE, 0);
            HttpContext context = HttpClientContext.create();
            CloseableHttpResponse httpResponse = httpClient.execute(method, context);
            httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                responseBody = EntityUtils.toString(httpEntity, "UTF-8");
            }

        } catch (Exception e) {
            if(method != null){
                method.abort();
            }
            e.printStackTrace();
            logger.error(
                    "execute post request exception, url:" + uri + ", exception:" + e.toString() + ", cost time(ms):"
                            + (System.currentTimeMillis() - startTime));
        } finally {
            if (httpEntity != null) {
                try {
                    EntityUtils.consumeQuietly(httpEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(
                            "close response exception, url:" + uri + ", exception:" + e.toString() + ", cost time(ms):"
                                    + (System.currentTimeMillis() - startTime));
                }
            }
        }
        return responseBody;
    }
    
	public static void main(String[] args) {
		System.out.println(HttpClientUtils.executePost("http://www.chebaba.com/couponapi/coupon/card-list?mobile=13719448937&sign=3b2a61950fe14a5369c4"));
	}
}
