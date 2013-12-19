/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.ifeng.util.imagecache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.widget.ImageView;

import com.ifeng.android.BuildConfig;
import com.ifeng.util.logging.Log;
import com.ifeng.util.net.ProxyHttpClient;

/**
 * A simple subclass of {@link ImageResizer} that fetches and resizes images
 * fetched from a URL.
 */
public class ImageFetcher extends ImageResizer {
	private static final String TAG = "ImageFetcher";
	private static final int HTTP_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
	private static final String HTTP_CACHE_DIR = "http";
	private static final int IO_BUFFER_SIZE = 8 * 1024;

	private DiskLruCache mHttpDiskCache;
	private File mHttpCacheDir;
	private boolean mHttpDiskCacheStarting = true;
	private final Object mHttpDiskCacheLock = new Object();
	private static final int DISK_CACHE_INDEX = 0;

	/**
	 * Initialize providing a target image width and height for the processing
	 * images.
	 * 
	 * @param context
	 * @param imageWidth
	 * @param imageHeight
	 * @param imageCacheTag
	 */
	public ImageFetcher(Context context, int imageWidth, int imageHeight,
			String imageCacheTag) {
		super(context, imageWidth, imageHeight);
		init(context, imageCacheTag);
	}

	/**
	 * Initialize providing a single target image size (used for both width and
	 * height);
	 * 
	 * @param context
	 * @param imageSize
	 * @param imageCacheTag
	 */
	public ImageFetcher(Context context, int imageSize, String imageCacheTag) {
		super(context, imageSize);
		init(context, imageCacheTag);
	}

	private void init(Context context, String tag) {
		checkConnection(context);
		mHttpCacheDir = ImageCache.getDiskCacheDir(context, tag
				+ File.separator + HTTP_CACHE_DIR);
	}

	@Override
	protected void initDiskCacheInternal() {
		super.initDiskCacheInternal();
		initHttpDiskCache();
	}

	private void initHttpDiskCache() {
		if (!mHttpCacheDir.exists()) {
			mHttpCacheDir.mkdirs();
		}
		synchronized (mHttpDiskCacheLock) {
			if (ImageCache.getUsableSpace(mHttpCacheDir) > HTTP_CACHE_SIZE) {
				try {
					mHttpDiskCache = DiskLruCache.open(mHttpCacheDir, 1, 1,
							HTTP_CACHE_SIZE);
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "HTTP cache initialized");
					}
				} catch (IOException e) {
					mHttpDiskCache = null;
				}
			}
			mHttpDiskCacheStarting = false;
			mHttpDiskCacheLock.notifyAll();
		}
	}

	@Override
	protected void clearCacheInternal() {
		super.clearCacheInternal();
		synchronized (mHttpDiskCacheLock) {
			if (mHttpDiskCache != null && !mHttpDiskCache.isClosed()) {
				try {
					mHttpDiskCache.delete();
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "HTTP cache cleared");
					}
				} catch (IOException e) {
					Log.e(TAG, "clearCacheInternal - " + e);
				}
				mHttpDiskCache = null;
				mHttpDiskCacheStarting = true;
				initHttpDiskCache();
			}
		}
	}

	@Override
	protected void flushCacheInternal() {
		super.flushCacheInternal();
		synchronized (mHttpDiskCacheLock) {
			if (mHttpDiskCache != null) {
				try {
					mHttpDiskCache.flush();
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "HTTP cache flushed");
					}
				} catch (IOException e) {
					Log.e(TAG, "flush - " + e);
				}
			}
		}
	}

	@Override
	protected void closeCacheInternal() {
		super.closeCacheInternal();
		synchronized (mHttpDiskCacheLock) {
			if (mHttpDiskCache != null) {
				try {
					if (!mHttpDiskCache.isClosed()) {
						mHttpDiskCache.close();
						mHttpDiskCache = null;
						if (BuildConfig.DEBUG) {
							Log.d(TAG, "HTTP cache closed");
						}
					}
				} catch (IOException e) {
					Log.e(TAG, "closeCacheInternal - " + e);
				}
			}
		}
	}

	/**
	 * Simple network connection check.
	 * 
	 * @param context
	 */
	private void checkConnection(Context context) {
		final ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
			// TODO 网络状态反馈
			// Toast.makeText(context, R.string.no_network_connection_toast,
			// Toast.LENGTH_LONG).show();
			Log.e(TAG, "checkConnection - no connection found");
		}
	}

	@Override
	public void loadImage(Object data, ImageView imageView) {
		loadImage((String) data, imageView,
				mImageHeight > mImageWidth ? mImageHeight : mImageWidth);
	}

	/**
	 * 加载指定缩放尺寸的图片
	 * 
	 * @param data
	 * @param callback
	 * @param imageSize
	 */
	public void loadImage(String data, ImageView imageView, int imageSize) {
		super.loadImageInternal(new RemoteBitmapTaskItem(data, imageSize),
				imageView);
	}

	@Override
	public void loadImage(Object data, BitmapWorkCallbackTaskContainer callback) {
		loadImage((String) data, callback,
				mImageHeight > mImageWidth ? mImageHeight : mImageWidth);
	}

	/**
	 * 获取指定缩放尺寸的图片
	 * 
	 * @param data
	 * @param callback
	 * @param imageSize
	 */
	public void loadImage(String data,
			BitmapWorkCallbackTaskContainer callback, int imageSize) {
		super.loadImageInternal(new RemoteBitmapTaskItem(data, imageSize),
				callback);
	}

	/**
	 * The main process method, which will be called by the ImageWorker in the
	 * AsyncTask background thread.
	 * 
	 * @param taskItem
	 *            The data to load the bitmap, in this case, a regular item
	 *            contain http URL
	 * @param processCallback
	 * 
	 * @return The downloaded and resized bitmap
	 */
	private Bitmap processBitmapInternal(RemoteBitmapTaskItem taskItem,
			OnProcessProgressUpdate processCallback) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "processBitmap - " + taskItem.mDataSource);
		}

		String url = (String) taskItem.mDataSource;
		final String key = ImageCache.hashKeyForDisk(url);
		FileDescriptor fileDescriptor = null;
		FileInputStream fileInputStream = null;
		DiskLruCache.Snapshot snapshot;
		synchronized (mHttpDiskCacheLock) {
			// Wait for disk cache to initialize
			while (mHttpDiskCacheStarting) {
				try {
					mHttpDiskCacheLock.wait();
				} catch (InterruptedException e) {
				}
			}
		}

		// 将editor提出，用于对可能在DiskLruCache加了锁的对象进行解锁。
		DiskLruCache.Editor editor = null;
		if (mHttpDiskCache != null) {
			try {
				snapshot = mHttpDiskCache.get(key);
				if (snapshot == null) {
					if (BuildConfig.DEBUG) {
						Log.d(TAG,
								"processBitmap, not found in http cache, downloading...");
					}
					editor = mHttpDiskCache.edit(key);
					if (editor != null) {
						if (downloadUrlToStream(url,
								editor.newOutputStream(DISK_CACHE_INDEX),
								processCallback)) {
							editor.commit();
						} else {
							editor.abort();
						}
					}
					snapshot = mHttpDiskCache.get(key);
				}
				if (snapshot != null) {
					fileInputStream = (FileInputStream) snapshot
							.getInputStream(DISK_CACHE_INDEX);
					fileDescriptor = fileInputStream.getFD();
				}
			} catch (IOException e) {
				Log.e(TAG, "processBitmap - " + e);
			} catch (IllegalStateException e) {
				Log.e(TAG, "processBitmap - " + e);
			} finally {
				if (fileDescriptor == null && fileInputStream != null) {
					try {
						fileInputStream.close();
					} catch (IOException e) {
					}
				}
			}
		}

		Bitmap bitmap = null;
		if (fileDescriptor != null) {
			bitmap = decodeSampledBitmapFromDescriptor(fileDescriptor,
					taskItem.mImageSize, taskItem.mImageSize, getImageCache());
		}

		if (fileInputStream != null) {
			try {
				fileInputStream.close();
			} catch (IOException e) {
			}
		}

		if (editor != null) {
			// 解锁DiskLruCache中的对象
			synchronized (editor) {
				editor.notify();
			}
		}

		return bitmap;
	}

	@Override
	protected Bitmap processBitmap(BitmapTaskItem taskItem,
			OnProcessProgressUpdate processCallback) {
		if (taskItem == null || !(taskItem instanceof RemoteBitmapTaskItem)) {
			Log.w(TAG, "ImageFecther need a RemoteBitmapTaskItem to process");
			return null;
		}
		return processBitmapInternal((RemoteBitmapTaskItem) taskItem,
				processCallback);
	}

	/**
	 * Download a bitmap from a URL and write the content to an output stream.
	 * 
	 * @param urlString
	 *            The URL to fetch
	 * @return true if successful, false otherwise
	 */
	public boolean downloadUrlToStream(String urlString,
			OutputStream outputStream, OnProcessProgressUpdate processCallback) {
		disableConnectionReuseIfNecessary();
		ProxyHttpClient client = null;
		BufferedOutputStream out = null;
		BufferedInputStream in = null;

		// 加入对加载限制的开关控制
		if (!isEnableNetworkProcess()) {
			return false;
		}

		try {
			client = new ProxyHttpClient(mContext);
			if (TextUtils.isEmpty(urlString)) {
				return false;
			}

			processCallback.updateProgress(0);

			HttpGet request = new HttpGet(urlString);
			HttpResponse response = client.execute(request);
			in = new BufferedInputStream(response.getEntity().getContent(),
					IO_BUFFER_SIZE);
			out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

			byte[] buffer = new byte[1024];
			int readLength;

			long readedLength = 0;
			long totalLength = response.getEntity().getContentLength();
			while ((readLength = in.read(buffer)) != -1) {
				out.write(buffer, 0, readLength);

				readedLength += readLength;
				// 更新进度
				if (totalLength != 0) {
					processCallback.updateProgress((int) ((float) readedLength
							/ totalLength * 100));
				}
			}

			processCallback.updateProgress(100);

			return true;
		} catch (final IOException e) {
			Log.e(TAG, "Error in downloadBitmap - " + e);
			Log.e(TAG, e);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "caught unknow exception - " + e);
		} finally {
			if (client != null) {
				client.close();
			}
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (final IOException e) {
			}
		}
		return false;
	}

	/**
	 * 是否可以进行网络请求加载图片
	 * 
	 * @return
	 */
	protected boolean isEnableNetworkProcess() {
		// 提供子类可对区分网络环境进行加载限制
		return true;
	}

	/**
	 * Workaround for bug pre-Froyo, see here for more info:
	 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 */
	public static void disableConnectionReuseIfNecessary() {
		// HTTP connection reuse which was buggy pre-froyo
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	/**
	 * 网络图片加载任务item
	 * 
	 * @author Calvin
	 * 
	 */
	private class RemoteBitmapTaskItem extends BitmapTaskItem {

		/** 图片缩放尺寸 */
		private int mImageSize;

		/**
		 * 构造
		 * 
		 * @param dataSource
		 * @param imageSize
		 */
		protected RemoteBitmapTaskItem(String dataSource, int imageSize) {
			super(dataSource);
			mImageSize = imageSize;
		}

	}
}
