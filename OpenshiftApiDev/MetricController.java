package ocp.api.controller;

import java.io.File;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import net.sf.json.JSONObject;
import ocp.api.config.CloudConfig;
import ocp.api.util.RemoteResourceUtil;

@RequestMapping(value="/metric")
@Controller
public class MetricController {

	private static Logger log = Logger.getLogger(DemoController.class);
	
	private final static String OSE_TRANS_PROTOCOL = "https";
	private static final int OSE_HTTPS_PORT = 8443;
	private static final String TOKEN_END_TAG = "</code>";
	private static final String TOKEN_START_TAG = "<code>";
	
	private static final String CLUSTER_USER_NAME = "cluster-admin";
	private static final String CLUSTER_USER_PWD = "welcome1";
	private static final String PROJECT_NAME = "demo";
	
	private static final String playload = 
			"{'bucketDuration':'1mn','start':'-1mn','tags':'descriptor_name:cpu/usage_rate,type:pod'}";
	
	private static final String POD_POST_URL = "https://hawkular-metrics.apps.example.com/hawkular/metrics/m/stats/query";
	
	
	/**
	 * 查询metrics指标信息
	 */
	@RequestMapping(value="/metrics")
	@ResponseBody
	public String  getMetricInfo(){
		String token = token();
		
		Object object;
		String json = "";
		try {
			object = postResources(POD_POST_URL, token, JSONObject.fromObject(playload).toString());
			
//			object = getResources("https://hawkular-metrics.apps.example.com/hawkular/metrics/gauges/mysql%2F25db61a4-d4b1-11e7-9e5f-000c29282e61%2Fcpu%2Fusage_rate/stats?bucketDuration=120000ms&start=-60mn", token);
			
			
			json = object.toString();
			
//			JSONObject jsonObject = JSONObject.fromObject(object);
//			json = jsonObject.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return json;
	}
	

	public String token() {
	    String token = null;
		try {
			String authUrl = CloudConfig.getInstance().getServiceEndpoint(CloudConfig.SVC_OAUTH_TOKEN);
			token = requestOAuthToken(authUrl, CLUSTER_USER_NAME, CLUSTER_USER_PWD);
			
		} catch (ConnectException e) {
			e.printStackTrace();
			log.error(MessageFormat.format("无法连接后台系统。用户 [{0}] 登录失败", "admin"));
		} catch (Exception e) {
			e.printStackTrace();
			log.error(MessageFormat.format("系统异常。用户 [{0}] 登录失败", "admin"));
		}
	    
		return token;
	}
	

	public static String requestOAuthToken(String url, String username, String password) throws Exception {
		DefaultHttpClient client = (DefaultHttpClient) genInsecureHttpClient();

		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
		String authHeader = "Basic " + new String(encodedAuth);

		HttpGet get = new HttpGet(url);
		if (CloudConfig.debug) {
			log.debug(url);
		}
		get.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

		HttpResponse rsp = client.execute(get);
		
		if (CloudConfig.debug) {
			log.debug(rsp.getStatusLine().getStatusCode());
		}
		
		String body = EntityUtils.toString(rsp.getEntity());
		
		if (CloudConfig.debug) {
			log.debug(body.toString());
		}

		int s = body.indexOf(TOKEN_START_TAG);
		int e = body.indexOf(TOKEN_END_TAG);

		String r = (s > 0 && e > s) ? r = body.substring(s + TOKEN_START_TAG.length(), e) : null;
		return r;
	}
	
	public static Object getResources(String url, String token) throws Exception {
		HttpClient client = genInsecureHttpClient();

		HttpGet get = new HttpGet(url);
		if (CloudConfig.debug) {
			log.debug(url);
		}

		get.setHeader("Authorization", "Bearer " + token);
		get.addHeader("Content-Type", "application/json; charset=UTF-8");

		get.addHeader("Accept", "application/json");
		get.addHeader("Hawkular-Tenant", PROJECT_NAME);
		
		HttpResponse rsp = client.execute(get);
		String body = EntityUtils.toString(rsp.getEntity(), "UTF-8");
		if (CloudConfig.debug) {
			log.debug(body);
		}

		return body;
	}
	
	
	public static Object postResources(String url, String token, String payload) throws Exception {
		HttpClient client = genInsecureHttpClient();

		HttpPost post = new HttpPost(url);
		if (CloudConfig.debug) {
			log.debug("postResources request");
			log.debug(url);
			log.debug(payload);
		}

		post.setHeader("Authorization", "Bearer " + token);
		post.addHeader("Content-Type", "application/json; charset=UTF-8");
		
		post.addHeader("Accept", "application/json");
		post.addHeader("Hawkular-Tenant", PROJECT_NAME);
		
		StringEntity e = new StringEntity(payload);
		post.setEntity(e);

		HttpResponse rsp = client.execute(post);
		
		String body = EntityUtils.toString(rsp.getEntity(), "UTF-8");
		if (CloudConfig.debug) {
			log.debug("Response");
			log.debug(body);
		}

		return body;
	}
	
	// For insure HTTP connection
	private static HttpClient genInsecureHttpClient() throws Exception {
		TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
			public boolean isTrusted(X509Certificate[] certificate, String authType) {
				return true;
			}
		};

		SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy,
				SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme(OSE_TRANS_PROTOCOL, OSE_HTTPS_PORT, sf));
		ClientConnectionManager ccm = new PoolingClientConnectionManager(registry);

		DefaultHttpClient hc = new DefaultHttpClient(ccm);
		return hc;
	}
		
}
