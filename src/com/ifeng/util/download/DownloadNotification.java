/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ifeng.util.download;

import java.util.Collection;
import java.util.HashMap;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.ifeng.android.R;

/**
 * This class handles the updating of the Notification Manager for the cases
 * where there is an ongoing download. Once the download is complete (be it
 * successful or unsuccessful) it is no longer the responsibility of this
 * component to show the download in the notification manager.
 * 
 */
class DownloadNotification {

	/** context used to access system services. */
	Context mContext;
	/** mNotifications. */
	HashMap<Long, NotificationItem> mNotifications;
	/** SystemFacade. */
	private SystemFacade mSystemFacade;

	/** log tag. */
	static final String LOGTAG = "DownloadNotification";
	/** WHERE_RUNNING. */
	static final String WHERE_RUNNING = "(" + Downloads.Impl.COLUMN_STATUS
			+ " >= '100') AND (" + Downloads.Impl.COLUMN_STATUS
			+ " <= '199') AND (" + Downloads.Impl.COLUMN_VISIBILITY
			+ " IS NULL OR " + Downloads.Impl.COLUMN_VISIBILITY + " == '"
			+ Downloads.Impl.VISIBILITY_VISIBLE + "' OR "
			+ Downloads.Impl.COLUMN_VISIBILITY + " == '"
			+ Downloads.Impl.VISIBILITY_VISIBLE_NOTIFY_COMPLETED + "')";
	/** WHERE_COMPLETED. */
	static final String WHERE_COMPLETED = Downloads.Impl.COLUMN_STATUS
			+ " >= '200' AND " + Downloads.Impl.COLUMN_VISIBILITY + " == '"
			+ Downloads.Impl.VISIBILITY_VISIBLE_NOTIFY_COMPLETED + "'";

	/**
	 * This inner class is used to collate downloads that are owned by the same
	 * application. This is so that only one notification line item is used for
	 * all downloads of a given application.
	 * 
	 */
	static class NotificationItem {
		/** // This first db _id for the download for the app */
		int mId;
		/** total current. */
		long mTotalCurrent = 0;
		/** total total. */
		long mTotalTotal = 0;
		/** mtitles. */
		String mTitle; // download title.
		/** paused text. */
		String mPausedText = null;

		/** Ticker text */
		String mTickerText = null;

		/** 是否是已暂停的下载 */
		boolean mPauseDownload = false;
	}

	/**
	 * Constructor
	 * 
	 * @param ctx
	 *            The context to use to obtain access to the Notification
	 *            Service
	 * @param systemFacade
	 *            SystemFacade
	 */
	DownloadNotification(Context ctx, SystemFacade systemFacade) {
		mContext = ctx;
		mSystemFacade = systemFacade;
		mNotifications = new HashMap<Long, NotificationItem>();
	}

	/**
	 * Update the notification ui.
	 * 
	 * @param downloads
	 *            downloads.
	 */
	public void updateNotification(Collection<DownloadInfo> downloads) {
		updateActiveNotification(downloads);
		updateCompletedNotification(downloads);
	}

	/**
	 * updateActiveNotification.
	 * 
	 * @param downloads
	 *            downloads
	 */
	private void updateActiveNotification(Collection<DownloadInfo> downloads) {
		// Collate the notifications
		mNotifications.clear();
		for (DownloadInfo download : downloads) {
			if (!isActiveAndVisible(download)) {
				continue;
			}

			// 暂停状态已经修改过Notification样式的，不再弹出notification
			if (download.mControl == Downloads.CONTROL_PAUSED) {
				if (download.mPauseNotiModifyFlag) {
					continue;
				} else {
					download.mPauseNotiModifyFlag = true;
				}
			}

			if (download.mStatus == Downloads.STATUS_PENDING) {
				continue;
			}
			// 由于增加了增量更新功能，所以进度显示有所变化，目前的进度是现在的大小占新包的大小。
			// 格式 newpackagesize@patchsize
			String extras = download.mExtras;
			long max = download.mTotalBytes;
			long progress = download.mCurrentBytes;
			if (!TextUtils.isEmpty(extras) && extras.contains("@")) {
				String[] array = extras.split("@");
				max = Integer.valueOf(array[0]);
				progress = max - Integer.valueOf(array[1])
						+ download.mCurrentBytes;
			}
			long id = download.mId;
			String title = download.mTitle;
			if (title == null || title.length() == 0) {
				// 首选 hint 文件名
				if (!TextUtils.isEmpty(download.mHint)) {
					title = download.mHint;
				} else {
					title = mContext.getResources().getString(
							R.string.download_unknown_title);
				}
			}

			NotificationItem item = new NotificationItem();
			item.mId = (int) id;
			item.mTitle = title;
			item.mTotalCurrent = progress;
			item.mTotalTotal = max;
			item.mPauseDownload = download.mControl == Downloads.CONTROL_PAUSED;
			mNotifications.put(id, item);

			if (download.mStatus == Downloads.Impl.STATUS_QUEUED_FOR_WIFI
					&& item.mPausedText == null) {
				item.mPausedText = mContext.getResources().getString(
						R.string.notification_need_wifi_for_size);
			} else if (download.mStatus == Downloads.Impl.STATUS_PAUSED_BY_APP
					&& item.mPausedText == null) {
				item.mPausedText = mContext.getResources().getString(
						R.string.download_paused);
			} else {
				item.mPausedText = null;
			}

		}

		// Add the notifications
		for (NotificationItem item : mNotifications.values()) {
			// Build the notification object
			VersionedNotification notification = VersionedNotification
					.getInstance(mContext);

			boolean hasPausedText = (item.mPausedText != null);
			int iconResource = android.R.drawable.stat_sys_download;
			if (hasPausedText) {
				iconResource = android.R.drawable.stat_sys_warning;
			}
			if (hasPausedText && item.mPauseDownload) {
				iconResource = android.R.drawable.stat_sys_download_done;
			}
			notification.setSmallIcon(iconResource);

			String title = item.mTitle;
			notification.setContentTitle(title);

			if (hasPausedText) {
				notification.setContentText(item.mPausedText);
			} else {
				notification.setProgress((int) item.mTotalTotal,
						(int) item.mTotalCurrent, item.mTotalTotal == -1);
				notification.setContentInfo(getDownloadingText(
						item.mTotalTotal, item.mTotalCurrent));
			}
			if (item.mTickerText == null) {
				item.mTickerText = item.mTitle
						+ " "
						+ mContext.getResources().getString(
								R.string.download_begin);
				if (item.mPauseDownload) {
					item.mTickerText = item.mTitle
							+ " "
							+ mContext.getResources().getString(
									R.string.download_paused);
				}
			}
			notification.setTicker(item.mTickerText);

			Intent intent = new Intent(Constants.ACTION_LIST);
			intent.setClassName(mContext.getPackageName(),
					DownloadReceiver.class.getName());
			intent.setData(ContentUris.withAppendedId(
					Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, item.mId));
			intent.putExtra("multiple", false);

			notification.setContentIntent(PendingIntent.getBroadcast(mContext,
					0, intent, 0));
			notification.setWhen(0);
			Notification n;
			if (item.mPauseDownload && hasPausedText) {
				notification.setOngoing(false);
				notification.setLatestEventInfo(true);
			} else {
				notification.setOngoing(true);
			}
			notification.setContentText(hasPausedText ? item.mPausedText : "");
			n = notification.getNotification();
			mSystemFacade.postNotification(item.mId, n);

		}
	}

	/**
	 * updateCompletedNotification.
	 * 
	 * @param downloads
	 *            DOWNLOADS
	 */
	private void updateCompletedNotification(Collection<DownloadInfo> downloads) {
		for (DownloadInfo download : downloads) {
			if (!isCompleteAndVisible(download)) {
				continue;
			}
			// Add the notifications
			Notification n = new Notification();
			n.icon = android.R.drawable.stat_sys_download_done;

			long id = download.mId;
			String title = download.mTitle;
			if (title == null || title.length() == 0) {
				// 首选 hint 文件名
				if (!TextUtils.isEmpty(download.mHint)) {
					title = download.mHint;
				} else {
					title = mContext.getResources().getString(
							R.string.download_unknown_title);
				}
			}

			Uri contentUri = ContentUris.withAppendedId(
					Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id);
			String caption;
			Intent intent;
			if (Downloads.Impl.isStatusError(download.mStatus)) {
				caption = mContext.getResources().getString(
						R.string.notification_download_failed);
				intent = new Intent(Constants.ACTION_REDOWNLOAD);
				intent.putExtra(Constants.DOWNLOAD_ID, download.mId);
				n.tickerText = title + caption; // 通知栏滚动提示
				n.icon = android.R.drawable.stat_sys_warning; // 下载失败
																// notification
																// icon.
			} else {
				caption = mContext.getResources().getString(
						R.string.notification_download_complete);
				if (download.mDestination == Downloads.Impl.DESTINATION_EXTERNAL) {
					intent = new Intent(Constants.ACTION_OPEN);
				} else {
					intent = new Intent(Constants.ACTION_LIST);
				}

				n.tickerText = title + caption; // 通知栏滚动提示
			}
			intent.setClassName(mContext.getPackageName(),
					DownloadReceiver.class.getName());
			intent.setData(contentUri);

			n.when = download.mLastMod;
			n.setLatestEventInfo(mContext, title, caption,
					PendingIntent.getBroadcast(mContext, 0, intent, 0));

			intent = new Intent(Constants.ACTION_HIDE);
			intent.setClassName(mContext.getPackageName(),
					DownloadReceiver.class.getName());
			intent.setData(contentUri);
			n.deleteIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);

			mSystemFacade.postNotification(download.mId, n);
		}
	}

	/**
	 * isActiveAndVisible
	 * 
	 * @param download
	 *            DownloadInfo
	 * @return is active and visibility == visible
	 */
	private boolean isActiveAndVisible(DownloadInfo download) {
		return 100 <= download.mStatus && download.mStatus < 200 // SUPPRESS
																	// CHECKSTYLE
				&& download.mVisibility != Downloads.VISIBILITY_HIDDEN;
	}

	/**
	 * isCompleteAndVisible
	 * 
	 * @param download
	 *            DownloadInfo
	 * @return is completed and visibility == visible
	 */
	private boolean isCompleteAndVisible(DownloadInfo download) {
		return download.mStatus >= 200 // SUPPRESS CHECKSTYLE
				&& download.mVisibility == Downloads.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
	}

	/**
	 * Helper function to build the downloading text.
	 * 
	 * @param totalBytes
	 *            totalBytes
	 * @param currentBytes
	 *            currentBytes
	 * @return the string
	 */
	private String getDownloadingText(long totalBytes, long currentBytes) {
		if (totalBytes <= 0) {
			return "";
		}
		long progress = currentBytes * 100 / totalBytes; // SUPPRESS CHECKSTYLE
		StringBuilder sb = new StringBuilder();
		sb.append(progress);
		sb.append('%');
		return sb.toString();
	}

}
