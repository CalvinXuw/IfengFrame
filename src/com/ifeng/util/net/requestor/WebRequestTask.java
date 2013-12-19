package com.ifeng.util.net.requestor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.Uri;
import android.os.Process;

import com.ifeng.util.logging.Log;
import com.ifeng.util.net.ProxyHttpClient;

/**
 * 从服务器获取数据的网络请求类
 * 
 * @author xuwei
 * 
 */
public class WebRequestTask extends RequestTask {

	/** log tag. */
	protected static final String TAG = WebRequestTask.class.getSimpleName();

	/** 访问网络失败时，重试间隔时间 */
	protected static final long SLEEP_TIME_WHILE_REQUEST_FAILED = 1000L;

	/**
	 * 参数提交方式
	 * 
	 * @author xuwei
	 * 
	 */
	public enum RequestType {
		/** GET方式提交 */
		GET,
		/** POST方式提交 */
		POST;
	}

	/**
	 * 参数提交试，默认Post提交
	 */
	private RequestType mRequestType = RequestType.POST;

	/**
	 * 请求尝试次数
	 */
	protected static final int TRY_COUNT = 3;

	/**
	 * 访问Url
	 */
	protected String mUrl;

	/**
	 * 头信息
	 */
	protected List<NameValuePair> mHeaders;

	/**
	 * 参数
	 */
	protected List<NameValuePair> mParams;

	/** context */
	protected Context mContext;

	/**
	 * 构造函数
	 * 
	 * @param context
	 *            Context
	 * @param url
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param listener
	 *            回调Listener
	 */
	public WebRequestTask(Context context, String url,
			List<NameValuePair> headers, List<NameValuePair> params,
			OnRequestTaskListener listener) {
		this(context, url, headers, params, Process.THREAD_PRIORITY_DEFAULT,
				listener);
	}

	/**
	 * 构造函数
	 * 
	 * @param context
	 *            Context
	 * @param url
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param priority
	 *            线程优先级
	 * @param listener
	 *            回调Listener
	 */
	public WebRequestTask(Context context, String url,
			List<NameValuePair> headers, List<NameValuePair> params,
			int priority, OnRequestTaskListener listener) {
		super(priority, listener);
		mContext = context.getApplicationContext();
		mUrl = url;
		mHeaders = headers;
		mParams = params;
	}

	/**
	 * 根据请求类型，分别进行不同的请求
	 * 
	 * @throws Exception
	 */
	@Override
	protected void processRequest() {
		if (DEBUG) {
			Log.d(TAG,
					"---- start web request time:" + System.currentTimeMillis());
		}

		if (mOnRequestTaskListener == null) {
			if (DEBUG) {
				Log.e(TAG, "web request listener is null");
			}
			return;
		}

		if (mUrl == null) {
			mOnRequestTaskListener
					.onFailed(IRequestModelErrorCode.ERROR_CODE_NO_URL);
			return;
		}

		int tryCount = 0;
		while (tryCount < TRY_COUNT) {
			ProxyHttpClient client = null;
			try {
				client = new ProxyHttpClient(mContext);
				String url = mUrl;

				HttpUriRequest request = null;
				if (mParams != null) {
					if (mRequestType == RequestType.POST) {
						request = new HttpPost(url);
						UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(
								mParams, HTTP.UTF_8);
						((HttpPost) request).setEntity(formEntity);
					} else {
						StringBuffer paramsStr = new StringBuffer();
						if (url.indexOf('?') < 0) {
							url = url + '?';
						}
						for (NameValuePair param : mParams) {
							if (paramsStr.length() != 0) {
								paramsStr.append('&');
							}
							paramsStr.append(param.getName()).append('=')
									.append(Uri.encode(param.getValue()));

						}
						request = new HttpGet(url + paramsStr);
						if (DEBUG) {
							Log.d(TAG, "---- requst url:" + (url + paramsStr));
						}
					}
				} else {
					request = new HttpPost(url);
					if (DEBUG) {
						Log.d(TAG, "---- requst url:" + url);
					}
				}

				if (mHeaders != null) {
					for (NameValuePair header : mHeaders) {
						request.setHeader(Uri.encode(header.getName()),
								Uri.encode(header.getValue()));
					}
				}

				HttpResponse response = client.execute(request);
				String content = null;

				boolean isGzip = false;
				Header header = response.getEntity().getContentEncoding();
				if (header != null) {
					for (HeaderElement element : header.getElements()) {
						if (element.getName().equalsIgnoreCase("gzip")) {
							isGzip = true;
							break;
						}
					}
				}
				if (isGzip) {
					GZIPInputStream gzIn = null;
					BufferedReader reader = null;
					try {
						gzIn = new GZIPInputStream(response.getEntity()
								.getContent());
						reader = new BufferedReader(new InputStreamReader(gzIn));
						StringBuffer str = new StringBuffer();
						String line = null;
						while ((line = reader.readLine()) != null) {
							str.append(line);
						}
						content = str.toString();
					} finally {
						if (reader != null) {
							reader.close();
						}
						if (gzIn != null) {
							gzIn.close();
						}
					}
				} else {
					content = EntityUtils.toString(response.getEntity());
				}
				if (DEBUG) {
					Log.d(TAG,
							"---- web request over time:"
									+ System.currentTimeMillis());
				}
				mOnRequestTaskListener.onSuccess(content);
				return;
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(SLEEP_TIME_WHILE_REQUEST_FAILED);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} finally {
				if (client != null) {
					client.close();
				}
			}

			tryCount++;
		}

		if (mOnRequestTaskListener != null) {
			mOnRequestTaskListener
					.onFailed(IRequestModelErrorCode.ERROR_CODE_NET_FAILED);
		}
	}

	/**
	 * @return the mRequestType
	 */
	public RequestType getRequestType() {
		return mRequestType;
	}

	/**
	 * @param requestType
	 *            the mRequestType to set
	 */
	public void setRequestType(RequestType requestType) {
		this.mRequestType = requestType;
	}

}
