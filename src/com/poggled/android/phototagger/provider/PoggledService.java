package com.poggled.android.phototagger.provider;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.format.DateUtils;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.poggled.android.phototagger.BuildConfig;
import com.poggled.android.phototagger.PhotoTaggerApplication;
import com.poggled.android.phototagger.io.StringParser;
import com.poggled.android.phototagger.util.TrustAllSSLSocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class PoggledService {
    
// production environment
    private static final String API_BASE_URL = "http://api.poggled.com";
    private static final String SECURE_API_BASE_URL = "https://api.poggled.com";
    public static final String BASE_URL = "http://www.poggled.com";
//     test environment
//    private static final String API_BASE_URL = "http://api.barswithfriends.com";
//    private static final String SECURE_API_BASE_URL = "https://api.barswithfriends.com";
    
    private static final String API_KEY = "f85e214f31dbd3af4c297d62a1b2bd7b";
    private static final String SECRET_KEY = "5094084018c184a7d25a71dc810bf5d6";
    
    private static final String POGGLED_SPACER = "~_~";
	
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";
	private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;
	private static final String HEADER_AUTH_TOKEN = "x-poggled-auth";
	private static final String HEADER_AUTH_TTL = "x-poggled-auth-ttl";
	private static final String HEADER_STORE_TOKEN = "x-poggled-store";
	private static final int HTTP_STATUS_INVALID_AUTH_TOKEN = 401;
	private static final int HTTP_STATUS_INVALID_STORE_TOKEN = 407;
	
	public static final int METHOD_GET = 0;
    public static final int METHOD_POST = 1;
    public static final int METHOD_PUT = 2;
    public static final int METHOD_DELETE = 3;
    
	private HttpClient mHttpClient;
	private Context mContext;

	public PoggledService(Context context) {
		mContext = context;
		mHttpClient = getHttpClient(context);
	}

	public boolean register(ArrayList<NameValuePair> params) throws Exception {
		final String url = SECURE_API_BASE_URL + "/users/signupWithFacebook";
		final String response = callService(METHOD_POST, url, params);
		//Card cards = CardParser.parseSingleCard(response);
		final boolean success = StringParser.parseSuccess(response);
		return success;
	}
	
	
	/**
	 * Generate and return a {@link HttpClient} configured for general use,
	 * including setting an application-specific user-agent string.
	 */
	public static HttpClient getHttpClient(Context context) {
		final HttpParams params = new BasicHttpParams();

		//HttpConnectionParams.setStaleCheckingEnabled(params, false);
		// Use generous timeouts for slow mobile networks
		HttpConnectionParams.setConnectionTimeout(params, 10 * SECOND_IN_MILLIS);
		HttpConnectionParams.setSoTimeout(params, 15 * SECOND_IN_MILLIS);
		ConnManagerParams.setTimeout(params, 10 * SECOND_IN_MILLIS);
		
		HttpConnectionParams.setSocketBufferSize(params, 8192);
		HttpProtocolParams.setUserAgent(params, buildUserAgent(context));
		

		//		params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
		//	    params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
		//	    params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
		//	    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		Scheme http = new Scheme("http", 
				PlainSocketFactory.getSocketFactory(), 80);
		schemeRegistry.register(http);
		
		if(BuildConfig.DEBUG) {
			KeyStore trustStore;
			try {
				trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				trustStore.load(null, null);
				SSLSocketFactory sf = new TrustAllSSLSocketFactory(trustStore);
				sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				schemeRegistry.register(new Scheme("https", sf, 443));
			}
			catch (Exception e) {
				Scheme https = new Scheme("https", 
						SSLSocketFactory.getSocketFactory(), 443);
				schemeRegistry.register(https);
				Log.e(PoggledService.class.getName(), "Problem creating debug SSL trust factory and keystore" + e.toString());
				
			}

		} else {
			Scheme https = new Scheme("https", 
					SSLSocketFactory.getSocketFactory(), 443);
			schemeRegistry.register(https);
		}


		ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(params, schemeRegistry); 
		//ClientConnectionManager connectionManager = new SingleClientConnManager(params, schemeRegistry); 

		final DefaultHttpClient client = new DefaultHttpClient(connectionManager, params);

		client.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				// Add header to accept gzip content
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
					if(BuildConfig.DEBUG)
						Log.d(getClass().getName(), "Adding header " + HEADER_ACCEPT_ENCODING + ": " + ENCODING_GZIP);
				}
			}
		});

		client.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext context) {
				// Inflate any responses compressed with gzip
				final HttpEntity entity = response.getEntity();
				final Header encoding = entity.getContentEncoding();

				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
							response.setEntity(new InflatingEntity(response.getEntity()));
							break;
						}
					}
				}
			}
		});

		return client;
	}

	private ArrayList<NameValuePair> constructPackage(ArrayList<NameValuePair> params) throws Exception {

		StringBuffer sb = new StringBuffer();

		sb.append(SECRET_KEY);

		params.add(new BasicNameValuePair("apikey", API_KEY));
		
		params.add(new BasicNameValuePair("device_id", PhotoTaggerApplication.id(mContext)));

		Collections.sort(params, new NameValueComparator());
		String previousKey = null;
		String currentKey = null;
		for (int i=0; i<params.size(); i++) {
			currentKey = params.get(i).getName();
			if (previousKey == null || currentKey.compareTo(previousKey) >= 0)
			{
				sb.append(POGGLED_SPACER);
				sb.append(params.get(i).getValue());
			}
			else 
			{
				throw new RuntimeException("Webservice package not formed correctly.  Check your parameter order.");
			}
			previousKey = currentKey;
		}

		String hashVal = sb.toString();
		String hashString = digestHash(hashVal);
		params.add(new BasicNameValuePair("hash", hashString));

		return params;

	}
	
	private String callService(int method, String url, ArrayList<NameValuePair> params) throws Exception
	{

	    HttpUriRequest request = null;
		
		switch (method) {
        case METHOD_GET:
        case METHOD_PUT:
        case METHOD_DELETE: {
            final StringBuffer sb = new StringBuffer();
            sb.append(url);

            // Add the parameters to the GET url if any
            if (params != null && !params.isEmpty()) {
                sb.append("?");

                for (NameValuePair nvp: params) {
                    final String key = nvp.getName();
                    
                    sb.append(URLEncoder.encode(key, "UTF-8"));
                    sb.append("=");
                    sb.append(URLEncoder.encode(nvp.getValue(), "UTF-8"));
                    sb.append("&");
                }
            }

            final URI uri = new URI(sb.toString());

            if (method == METHOD_GET) {
                request = new HttpGet(uri);
            } else if (method == METHOD_PUT) {
                request = new HttpPut(uri);
            } else if (method == METHOD_DELETE) {
                request = new HttpDelete(uri);
            }
            break;
        }
        case METHOD_POST: {
            final URI uri = new URI(url);
            request = new HttpPost(uri);

            // Add the parameters to the POST request if any
            if (params != null && !params.isEmpty()) {

                //request.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");
                ((HttpPost) request).setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            } 
            break;
        }
        default: {
            throw new IllegalArgumentException(
                    "Request method must be METHOD_GET, METHOD_POST, METHOD_PUT or METHOD_DELETE");
        }
    }
		try {
			
			if(BuildConfig.DEBUG) {
			    if(method == METHOD_POST) {
			        Log.d(getClass().getName(), "Http request: " + request.getURI() + "?" + EntityUtils.toString(((HttpPost) request).getEntity()));
			    } else {
			        Log.d(getClass().getName(), "Http request: " + request.getURI());
			    }
			}
			HttpResponse httpResponse = mHttpClient.execute(request);
			final int statusCode = httpResponse.getStatusLine().getStatusCode();

			if(BuildConfig.DEBUG)
				Log.d(getClass().getName(), "Status: " + httpResponse.getStatusLine());

			
			Header[] headers = httpResponse.getAllHeaders();
			if(BuildConfig.DEBUG) {
    			for(Header h : headers) {
    			    Log.d(getClass().getName(), "Header " + h.getName() + ": " + h.getValue());
    			}
			}
				
//			if (statusCode != HttpStatus.SC_OK) {
//				return null;
//    			}
    
			HttpEntity responseEntity = httpResponse.getEntity();

			if (responseEntity != null) {
				final String response =  EntityUtils.toString(responseEntity);
				if(BuildConfig.DEBUG)
					Log.d(getClass().getName(), "Response: " + response);
				return response;
			}
    
    		} catch (ConnectTimeoutException e) {
    		    request.abort();
    		    Log.e(getClass().getName(), "Problem while connecting to web service", e);
    		    throw new RuntimeException("Problem while connecting to web service. Please try again.");
    		} catch (IOException e) {
    
    			request.abort();
    			Log.e(getClass().getName(), "Problem while connecting to web service", e);
    			throw new RuntimeException("Problem while connecting to web service. Please try again.");
    
    		} catch (Exception e) {
    			Log.e(getClass().getName(), "Problem while connecting to web service", e);
    			throw new RuntimeException("Problem while connecting to web service. Please try again.");
    
    		}
    

		return null;
	}

	
	public static String digestHash(String hashString) {

	    String hashVal = hashString;

	    StringBuffer hb = new StringBuffer();
	    try {
	        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
	        digest.update(hashVal.getBytes());
	        byte messageDigest[] = digest.digest();

	        for (int i = 0; i < messageDigest.length; i++) {
	            String hex = Integer.toHexString(0xff & messageDigest[i]);
	            if (hex.length() == 1)
	                hb.append('0');
	            hb.append(hex);
	        }
	        return hashString = hb.toString();
	    }
	    catch (NoSuchAlgorithmException e) {
	        return null;
	    }
	}
	
	/**
	 * Build and return a user-agent string that can identify this application
	 * to remote servers. Contains the package name and version code.
	 */
	private static String buildUserAgent(Context context) {
		try {
			final PackageManager manager = context.getPackageManager();
			final PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);

			// Some APIs require "(gzip)" in the user-agent string.
			return info.packageName + "/" + info.versionName
			+ " (" + info.versionCode + ") (gzip)";
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	/**
	 * Simple {@link HttpEntityWrapper} that inflates the wrapped
	 * {@link HttpEntity} by passing it through {@link GZIPInputStream}.
	 */
	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}

	private static class NameValueComparator implements Comparator<NameValuePair> {
		public int compare(NameValuePair pairA, NameValuePair pairB)
		{
			return pairA.getName().compareTo(pairB.getName());
		}
	}
}
