package com.ifeng.util.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;

import com.ifeng.BaseApplicaion;

/**
 * 联网管理类
 * 
 * @author Calvin
 * 
 */
public class ConnectManager {

	/** tag */
	private final String TAG = getClass().getSimpleName();
	/** debug */
	private static final boolean DEBUG = BaseApplicaion.DEBUG;
	private String mApn;
	private String mProxy;
	private String mPort;
	private boolean mUseWap;
	private String mNetType;

	public ConnectManager(Context context) {
		checkNetworkType(context);
	}

	private void checkApn(Context context, NetworkInfo networkinfo) {
		if (networkinfo.getExtraInfo() != null) {
			String s = networkinfo.getExtraInfo().toLowerCase();
			if (s != null) {
				if (s.startsWith("cmwap") || s.startsWith("uniwap")
						|| s.startsWith("3gwap")) {
					mUseWap = true;
					mApn = s;
					mProxy = "10.0.0.172";
					mPort = "80";
					return;
				}
				if (s.startsWith("ctwap")) {
					mUseWap = true;
					mApn = s;
					mProxy = "10.0.0.200";
					mPort = "80";
					return;
				}
				if (s.startsWith("cmnet") || s.startsWith("uninet")
						|| s.startsWith("ctnet") || s.startsWith("3gnet")) {
					mUseWap = false;
					mApn = s;
					return;
				}
			}
		}
		String s1 = Proxy.getDefaultHost();
		int i = Proxy.getDefaultPort();
		if (s1 != null && s1.length() > 0) {
			mProxy = s1;
			if ("10.0.0.172".equals(mProxy.trim())) {
				mUseWap = true;
				mPort = "80";
			} else if ("10.0.0.200".equals(mProxy.trim())) {
				mUseWap = true;
				mPort = "80";
			} else {
				mUseWap = false;
				mPort = Integer.toString(i);
			}
		} else {
			mUseWap = false;
		}
	}

	private void checkNetworkType(Context context) {
		ConnectivityManager connectivitymanager = (ConnectivityManager) context
				.getApplicationContext().getSystemService("connectivity");
		NetworkInfo networkinfo = connectivitymanager.getActiveNetworkInfo();
		if (networkinfo != null)
			if ("wifi".equals(networkinfo.getTypeName().toLowerCase())) {
				mNetType = "wifi";
				mUseWap = false;
			} else {
				checkApn(context, networkinfo);
				mNetType = mApn;
			}
	}

	public static boolean isNetworkConnected(Context context) {
		ConnectivityManager connectivitymanager = (ConnectivityManager) context
				.getApplicationContext().getSystemService("connectivity");
		NetworkInfo networkinfo = connectivitymanager.getActiveNetworkInfo();
		if (networkinfo != null)
			return networkinfo.isConnectedOrConnecting();
		else
			return false;
	}

	public boolean isWapNetwork() {
		return mUseWap;
	}

	public String getApn() {
		return mApn;
	}

	public String getProxy() {
		return mProxy;
	}

	public String getProxyPort() {
		return mPort;
	}

	public String getNetType() {
		return mNetType;
	}

}