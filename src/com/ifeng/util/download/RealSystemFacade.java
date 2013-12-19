package com.ifeng.util.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.ifeng.util.logging.Log;

/**
 * the real System Facade implements.
 */
class RealSystemFacade implements SystemFacade {
	/** app context used to access system interfaces. */
	private Context mContext;

	/** NotificationManager . */
	private NotificationManager mNotificationManager;

	/**
	 * constructor.
	 * 
	 * @param context
	 *            Context
	 */
	public RealSystemFacade(Context context) {
		mContext = context;
		mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	/**
	 * get current network type.
	 * 
	 * @return see NetworkInfo.getType
	 */
	public Integer getActiveNetworkType() {
		ConnectivityManager connectivity = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.w(Constants.TAG, "couldn't get connectivity manager");
			return null;
		}

		for (NetworkInfo info : connectivity.getAllNetworkInfo()) {
			if (info != null && info.isConnectedOrConnecting()) {
				return info.getType();
			}
		}

		return null;
	}

	/**
	 * is Network Roaming.
	 * 
	 * @return Roaming network return true.
	 */
	public boolean isNetworkRoaming() {
		ConnectivityManager connectivity = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.w(Constants.TAG, "couldn't get connectivity manager");
			return false;
		}

		NetworkInfo info = connectivity.getActiveNetworkInfo();
		boolean isMobile = (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE);
		TelephonyManager tm = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		boolean isRoaming = isMobile && tm.isNetworkRoaming();
		if (Constants.LOGVV && isRoaming) {
			Log.v(Constants.TAG, "network is roaming");
		}
		return isRoaming;
	}

	/**
	 * the max allowed file size throw the mobile network.
	 * 
	 * @return the max allowed file size throw the mobile network.
	 */
	public Long getMaxBytesOverMobile() {
		// TODO 取消对于漫游网络的下载限制
		return Long.MAX_VALUE;
		// try {
		// return Settings.Secure.getLong(mContext.getContentResolver(),
		// "download_manager_max_bytes_over_mobile"/*
		// * Settings.Secure.
		// * DOWNLOAD_MAX_BYTES_OVER_MOBILE
		// */);
		// } catch (SettingNotFoundException exc) {
		// return null;
		// }
	}

	@Override
	public Long getRecommendedMaxBytesOverMobile() {
		// TODO 取消对于漫游网络的下载限制
		return Long.MAX_VALUE;
		// try {
		// return Settings.Secure.getLong(mContext.getContentResolver(),
		// "download_manager_recommended_max_bytes_over_mobile"
		// /* Settings.Secure.DOWNLOAD_RECOMMENDED_MAX_BYTES_OVER_MOBILE */);
		// } catch (SettingNotFoundException exc) {
		// return null;
		// }
	}

	@Override
	public void sendBroadcast(Intent intent) {
		mContext.sendBroadcast(intent);
	}

	@Override
	public boolean userOwnsPackage(int uid, String packageName)
			throws NameNotFoundException {
		return mContext.getPackageManager().getApplicationInfo(packageName, 0).uid == uid;
	}

	@Override
	public void postNotification(long id, Notification notification) {
		/**
		 * TODO: The system notification manager takes ints, not longs, as IDs,
		 * but the download manager uses IDs take straight from the database,
		 * which are longs. This will have to be dealt with at some point.
		 */
		try {
			mNotificationManager.notify((int) id, notification);
		} catch (Exception e) {
			if (Constants.LOGV) {
				Log.e(Constants.TAG, "postNotification:" + e.getMessage());
			}
		}
	}

	@Override
	public void cancelNotification(long id) {
		mNotificationManager.cancel((int) id);
	}

	@Override
	public void cancelAllNotifications() {
		mNotificationManager.cancelAll();
	}

	@Override
	public void startThread(Thread thread) {
		thread.start();
	}
}
