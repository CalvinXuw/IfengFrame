package com.ifeng.util.net;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.text.TextUtils;

import com.ifeng.BaseApplicaion;
import com.ifeng.util.logging.Log;

public class ProxyHttpClient extends DefaultHttpClient {
	private final String TAG = getClass().getSimpleName();
	private static final boolean DEBUG = BaseApplicaion.DEBUG;
	private String mProxy;
	private String mPort;
	private boolean mUseWap;
	private static final int HTTP_TIMEOUT_MS = 15000;
	private RuntimeException mLeakedException = new IllegalStateException(
			"ProxyHttpClient created and never closed");

	public ProxyHttpClient(Context paramContext) {
		this(paramContext, null, null);
	}

	public ProxyHttpClient(Context paramContext, String paramString) {
		this(paramContext, paramString, null);
	}

	public ProxyHttpClient(Context paramContext,
			ConnectManager paramConnectManager) {
		this(paramContext, null, paramConnectManager);
	}

	public ProxyHttpClient(Context paramContext, String paramString,
			ConnectManager paramConnectManager) {
		ConnectManager localConnectManager = paramConnectManager;
		if (localConnectManager == null)
			localConnectManager = new ConnectManager(paramContext);
		this.mUseWap = localConnectManager.isWapNetwork();
		this.mProxy = localConnectManager.getProxy();
		this.mPort = localConnectManager.getProxyPort();
		if ((this.mProxy != null) && (this.mProxy.length() > 0)) {
			HttpHost localHttpHost = new HttpHost(this.mProxy, Integer.valueOf(
					this.mPort).intValue());
			getParams().setParameter("http.route.default-proxy", localHttpHost);
		}
		HttpConnectionParams.setConnectionTimeout(getParams(), HTTP_TIMEOUT_MS);
		HttpConnectionParams.setSoTimeout(getParams(), HTTP_TIMEOUT_MS);
		HttpConnectionParams.setSocketBufferSize(getParams(), 8192);
		if (!TextUtils.isEmpty(paramString))
			HttpProtocolParams.setUserAgent(getParams(), paramString);
	}

	protected void finalize() throws Throwable {
		super.finalize();
		if (this.mLeakedException != null)
			Log.e(TAG, "Leak found", this.mLeakedException);
	}

	public void close() {
		if (this.mLeakedException != null) {
			getConnectionManager().shutdown();
			this.mLeakedException = null;
		}
	}

	public boolean isWap() {
		return this.mUseWap;
	}

	protected HttpParams createHttpParams() {
		HttpParams localHttpParams = super.createHttpParams();
		HttpProtocolParams.setUseExpectContinue(localHttpParams, false);
		return localHttpParams;
	}
}