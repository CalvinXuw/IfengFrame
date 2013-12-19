package com.ifeng.util.net.requestor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.Uri;
import android.os.Process;

import com.ifeng.util.logging.Log;
import com.ifeng.util.net.ProxyHttpClient;

/**
 * 上传文件的网络请求类
 * 
 * @author xuwei
 * 
 */
public class UploadRequestTask extends WebRequestTask {

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
	public UploadRequestTask(Context context, String url,
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
	public UploadRequestTask(Context context, String url,
			List<NameValuePair> headers, List<NameValuePair> params,
			int priority, OnRequestTaskListener listener) {
		super(context, url, params, params, priority, listener);
	}

	/**
	 * 处理文件上传请求
	 * 
	 * @throws Exception
	 */
	@Override
	protected void processRequest() {
		if (DEBUG) {
			Log.d(TAG,
					"---- start upload request time:"
							+ System.currentTimeMillis());
		}

		if (mOnRequestTaskListener == null) {
			if (DEBUG) {
				Log.e(TAG, "upload request listener is null");
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

				HttpPost request = new HttpPost(url);
				MultipartEntityBuilder multipartEntity = MultipartEntityBuilder
						.create();
				multipartEntity.setCharset(Charset.forName(HTTP.UTF_8));
				multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

				if (mParams != null) {
					for (NameValuePair pair : mParams) {
						if (pair instanceof FileValuePair) {
							multipartEntity.addBinaryBody(pair.getName(),
									new File(pair.getValue()));
						} else {
							multipartEntity.addTextBody(pair.getName(),
									pair.getValue());
						}
					}
				}

				request.setEntity(multipartEntity.build());

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
							"---- upload request over time:"
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
	 * 传递上传文件参数
	 * 
	 * @author Calvin
	 * 
	 */
	public static class FileValuePair extends BasicNameValuePair {

		/**
		 * 构造
		 * 
		 * @param name
		 * @param filepath
		 */
		public FileValuePair(String name, String filepath) {
			super(name, filepath);
		}

	}
}
