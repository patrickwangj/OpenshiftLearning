package ocp.api.controller;

import java.net.ConnectException;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sun.jna.platform.win32.DsGetDC.DOMAIN_CONTROLLER_INFO;

import net.sf.json.JSONObject;
import ocp.api.config.CloudConfig;
import ocp.api.util.RemoteResourceUtil;

@RequestMapping(value="/autoscaling")
@Controller
public class AutoScalingController {
	private static Logger log = Logger.getLogger(AutoScalingController.class);

	private final static String OSE_TRANS_PROTOCOL = "https";
	private static final int OSE_HTTPS_PORT = 8443;
	private static final String TOKEN_END_TAG = "</code>";
	private static final String TOKEN_START_TAG = "<code>";
	
	private static final String DOMAIN = "https://master.ynyx.com:8443";
	private static final String USER_NAME = "admin";
	private static final String USER_PWD = "welcome1";
	
	@RequestMapping(value = "/token.do")
	@ResponseBody
	public String token() {
	    String token = null;
		try {
			String authUrl = DOMAIN + "/oauth/token/request";
			token = requestOAuthToken(authUrl, USER_NAME, USER_PWD);
			
		} catch (ConnectException e) {
			e.printStackTrace();
			log.error(MessageFormat.format("无法连接后台系统。用户 [{0}] 登录失败", "admin"));
		} catch (Exception e) {
			e.printStackTrace();
			log.error(MessageFormat.format("系统异常。用户 [{0}] 登录失败", "admin"));
		}
	    
		return token;
	}
	
	
	/**
	 * Create a HorizontalPodAutoscaler in a namespace
	 * 
	 * 这里只给出一个demo，其他操作请参阅  https://github.com/openshift/origin/blob/master/api/docs/apis-autoscaling/v1.HorizontalPodAutoscaler.adoc
	 */
	@RequestMapping(value="/{namespace}.do")
	@ResponseBody
	public String  scaling(@PathVariable(value = "namespace") String namespace){
		String token = token();
		
		Object object;
		String json = "";
		try {
//			object = RemoteResourceUtil.getResources("https://master.ynyx.com:8443/api/v1/model/metrics/memory/node_capacity?start=2017-11-27T21:13:00&end=2017-11-27T21:15:59", token);
			String playload = "{\"apiVersion\": 'autoscaling/v1',\"kind\": 'HorizontalPodAutoscaler',\"metadata\":{\"name\": 'frontend'}, \"spec\":{\"scaleTargetRef\":{\"kind\": 'DeploymentConfig', \"name\": 'autoscaling-mysql' ,\"apiVersion\": 'v1'},\"minReplicas\": 1,\"maxReplicas\": 10, \"targetCPUUtilizationPercentage\":  20}}";
			
			
			
			object = RemoteResourceUtil.postResources(DOMAIN + "/apis/autoscaling/v1/namespaces/" + namespace + "/horizontalpodautoscalers", token, JSONObject.fromObject(playload).toString());
			JSONObject jsonObject = JSONObject.fromObject(object);
			json = jsonObject.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return json;
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
