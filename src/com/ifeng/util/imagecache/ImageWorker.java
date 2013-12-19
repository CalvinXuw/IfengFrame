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

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.widget.ImageView;

import com.ifeng.BaseApplicaion;
import com.ifeng.android.BuildConfig;
import com.ifeng.util.logging.Log;

/**
 * This class wraps up completing some arbitrary long running work when loading
 * a bitmap to an ImageView. It handles things like using a memory and disk
 * cache, running the work in a background thread and setting a placeholder
 * image.
 */
public abstract class ImageWorker {
	private static final String TAG = "ImageWorker";
	/** if enabled, logcat will output the log. */
	protected final boolean DEBUG = true & BaseApplicaion.DEBUG;

	private static final int FADE_IN_TIME = 200;

	private ImageCache mImageCache;
	private ImageCache.ImageCacheParams mImageCacheParams;
	private Bitmap mLoadingBitmap;
	private boolean mFadeInBitmap = true;
	private boolean mExitTasksEarly = false;
	protected boolean mPauseWork = false;
	private final Object mPauseWorkLock = new Object();

	protected Resources mResources;

	private static final int MESSAGE_CLEAR = 0;
	private static final int MESSAGE_INIT_DISK_CACHE = 1;
	private static final int MESSAGE_FLUSH = 2;
	private static final int MESSAGE_CLOSE = 3;
	private static final int MESSAGE_CLEAR_MEMORY = 4;
	/** 供fetcher中构成httpclient使用 */
	protected Context mContext;
	/** 在回调请求方式中添加同步锁，避免类似像在webview中替换js代码的时候顺序产生错乱 */
	private Object mCallbackSyncLock = new Object();

	/** 图片加载线程池 */
	private Executor mExecutor = Executors.newFixedThreadPool(2);

	protected ImageWorker(Context context) {
		mResources = context.getResources();
		mContext = context.getApplicationContext();
	}

	/**
	 * 加载图片到ImageView上，其中data可传入图片url地址或者图片resId。其中默认构造一个 {@link BitmapTaskItem}
	 * ，子类如需实现具体的处理，可重写此方法，传入不同的 {@link BitmapTaskItem}从而在
	 * {@link #processBitmap(Object)}方法体中进行具体实现
	 * 
	 * @param data
	 * @param imageView
	 */
	public void loadImage(Object data, ImageView imageView) {
		loadImageInternal(new BitmapTaskItem(data), imageView);
	}

	/**
	 * Load an image specified by the data parameter into an ImageView (override
	 * {@link ImageWorker#processBitmap(Object)} to define the processing
	 * logic). A memory and disk cache will be used if an {@link ImageCache} has
	 * been added using
	 * {@link ImageWorker#addImageCache(FragmentManager, ImageCache.ImageCacheParams)}
	 * . If the image is found in the memory cache, it is set immediately,
	 * otherwise an {@link AsyncTask} will be created to asynchronously load the
	 * bitmap.
	 * 
	 * @param item
	 *            The loadtask of the image to download.
	 * @param imageView
	 *            The ImageView to bind the downloaded image to.
	 */
	final protected void loadImageInternal(BitmapTaskItem item,
			ImageView imageView) {
		if (item == null || item.mDataSource == null || imageView == null) {
			return;
		}

		// 字符串的非空排查
		if (item.mDataSource instanceof String
				&& TextUtils.isEmpty((CharSequence) item.mDataSource)) {
			return;
		}
		// resId的非空排查s
		if (item.mDataSource instanceof Integer
				&& (Integer) item.mDataSource == 0) {
			return;
		}

		BitmapDrawable value = null;

		if (mImageCache != null) {
			value = mImageCache.getBitmapFromMemCache(String
					.valueOf(item.mDataSource));
		}

		if (value != null) {
			// Bitmap found in memory cache
			// Bug fix by XuWei 2013-09-09
			// 若直接调用缓存中Drawable则会产生图片大小尺寸异常的bug，复用了其他View缩放好的Drawable，故重新生成新的Drawable
			Drawable copyDrawable = new BitmapDrawable(mResources,
					value.getBitmap());
			imageView.setImageDrawable(copyDrawable);
		} else if (cancelPotentialWork(item.mDataSource, imageView)) {

			byte[] chunk = null;
			if (mLoadingBitmap != null) {
				chunk = mLoadingBitmap.getNinePatchChunk();
			}

			final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
			if (chunk == null) {
				final AsyncBitmapDrawable asyncBitmapDrawable = new AsyncBitmapDrawable(
						mResources, mLoadingBitmap, task);
				imageView.setImageDrawable(asyncBitmapDrawable);
			} else {
				final AsyncNinePatchDrawable.NinePatchChunk npc = AsyncNinePatchDrawable.NinePatchChunk
						.deserialize(chunk);
				final AsyncNinePatchDrawable asyncNinePatchDrawable = new AsyncNinePatchDrawable(
						mResources, mLoadingBitmap, chunk, npc.mPaddings, null,
						task);
				imageView.setImageDrawable(asyncNinePatchDrawable);
			}

			// NOTE: This uses a custom version of AsyncTask that has been
			// pulled from the
			// framework and slightly modified. Refer to the docs at the top of
			// the class
			// for more info on what was changed.
			task.executeOnExecutor(mExecutor, item);
		}
	}

	/**
	 * Set placeholder bitmap that shows when the the background thread is
	 * running.
	 * 
	 * @param bitmap
	 */
	public void setLoadingImage(Bitmap bitmap) {
		mLoadingBitmap = bitmap;
	}

	/**
	 * Set placeholder bitmap that shows when the the background thread is
	 * running.
	 * 
	 * @param resId
	 */
	public void setLoadingImage(int resId) {
		mLoadingBitmap = BitmapFactory.decodeStream(mResources
				.openRawResource(resId));
	}

	/**
	 * Adds an {@link ImageCache} to this {@link ImageWorker} to handle disk and
	 * memory bitmap caching.
	 * 
	 * @param fragmentManager
	 * @param cacheParams
	 *            The cache parameters to use for the image cache.
	 */
	public void addImageCache(FragmentManager fragmentManager,
			ImageCache.ImageCacheParams cacheParams) {
		mImageCacheParams = cacheParams;
		mImageCache = ImageCache
				.getInstance(fragmentManager, mImageCacheParams);
		new CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE);
	}

	/**
	 * Adds an {@link ImageCache} to this {@link ImageWorker} to handle disk and
	 * memory bitmap caching.
	 * 
	 * @param activity
	 * @param diskCacheDirectoryName
	 *            See
	 *            {@link ImageCache.ImageCacheParams#ImageCacheParams(Context, String)}
	 *            .
	 */
	public void addImageCache(FragmentActivity activity,
			String diskCacheDirectoryName) {
		mImageCacheParams = new ImageCache.ImageCacheParams(activity,
				diskCacheDirectoryName);
		mImageCache = ImageCache.getInstance(
				activity.getSupportFragmentManager(), mImageCacheParams);
		new CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE);
	}

	/**
	 * If set to true, the image will fade-in once it has been loaded by the
	 * background thread.
	 */
	public void setImageFadeIn(boolean fadeIn) {
		mFadeInBitmap = fadeIn;
	}

	public void setExitTasksEarly(boolean exitTasksEarly) {
		mExitTasksEarly = exitTasksEarly;
		setPauseWork(false);
	}

	/**
	 * Subclasses should override this to define any processing or work that
	 * must happen to produce the final bitmap. This will be executed in a
	 * background thread and be long running. For example, you could resize a
	 * large bitmap here, or pull down an image from the network.
	 * 
	 * @param taskItem
	 *            The data to identify which image to process, as provided by
	 *            {@link ImageWorker#loadImageInternal(BitmapTaskItem, ImageView)}
	 * @param processCallback
	 *            the callback of process progress
	 * @return The processed bitmap
	 */
	protected abstract Bitmap processBitmap(BitmapTaskItem taskItem,
			OnProcessProgressUpdate processCallback);

	/**
	 * @return The {@link ImageCache} object currently being used by this
	 *         ImageWorker.
	 */
	protected ImageCache getImageCache() {
		return mImageCache;
	}

	/**
	 * 设置图片并加载淡入动画
	 * 
	 * @param imageView
	 * @param drawable
	 */
	public static void setFadeInDrawable(ImageView imageView, Drawable drawable) {
		// Bug fix by XuWei 2013-09-09
		// 若直接调用缓存中Drawable则会产生图片大小尺寸异常的bug，复用了其他View缩放好的Drawable，故重新生成新的Drawable
		Drawable copyDrawable = new BitmapDrawable(imageView.getContext()
				.getResources(), ((BitmapDrawable) drawable).getBitmap());

		// Transition drawable with a transparent drawable and the final
		// drawable
		final TransitionDrawable td = new TransitionDrawable(new Drawable[] {
				new ColorDrawable(android.R.color.transparent), copyDrawable });

		imageView.setImageDrawable(td);
		td.startTransition(FADE_IN_TIME);
	}

	/**
	 * Cancels any pending work attached to the provided ImageView.
	 * 
	 * @param imageView
	 */
	public static void cancelWork(ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
		if (bitmapWorkerTask != null) {
			bitmapWorkerTask.cancel(true);
			if (BuildConfig.DEBUG) {
				final Object bitmapData = bitmapWorkerTask.mBitmapTaskItem.mDataSource;
				Log.d(TAG, "cancelWork - cancelled work for " + bitmapData);
			}
		}
	}

	/**
	 * Returns true if the current work has been canceled or if there was no
	 * work in progress on this image view. Returns false if the work in
	 * progress deals with the same data. The work is not stopped in that case.
	 */
	public static boolean cancelPotentialWork(Object data, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final BitmapTaskItem bitmapTaskItem = bitmapWorkerTask.mBitmapTaskItem;
			if (bitmapTaskItem == null || bitmapTaskItem.mDataSource == null
					|| !bitmapTaskItem.mDataSource.equals(data)) {
				bitmapWorkerTask.cancel(true);
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "cancelPotentialWork - cancelled work for "
							+ data);
				}
			} else {
				// The same work is already in progress.
				return false;
			}
		}
		return true;
	}

	/**
	 * @param imageView
	 *            Any imageView
	 * @return Retrieve the currently active work task (if any) associated with
	 *         this imageView. null if there is no such task.
	 */
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncBitmapDrawable) {
				final AsyncBitmapDrawable asyncDrawable = (AsyncBitmapDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			} else if (drawable instanceof AsyncNinePatchDrawable) {
				final AsyncNinePatchDrawable asyncDrawable = (AsyncNinePatchDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	/**
	 * The actual AsyncTask that will asynchronously process the image.
	 */
	private class BitmapWorkerTask extends
			AsyncTask<BitmapTaskItem, Integer, BitmapDrawable> implements
			OnProcessProgressUpdate {

		/** 图片数据源，缓存标识 */
		private BitmapTaskItem mBitmapTaskItem;
		private final WeakReference<ImageView> imageViewReference;

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		/**
		 * Background processing.
		 */
		@Override
		protected BitmapDrawable doInBackground(BitmapTaskItem... params) {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "doInBackground - starting work");
			}

			mBitmapTaskItem = params[0];
			final String dataString = String
					.valueOf(mBitmapTaskItem.mDataSource);
			Bitmap bitmap = null;
			BitmapDrawable drawable = null;

			// Wait here if work is paused and the task is not cancelled
			synchronized (mPauseWorkLock) {
				while (mPauseWork && !isCancelled()) {
					try {
						mPauseWorkLock.wait();
					} catch (InterruptedException e) {
					}
				}
			}

			// If the image cache is available and this task has not been
			// cancelled by another
			// thread and the ImageView that was originally bound to this task
			// is still bound back
			// to this task and our "exit early" flag is not set then try and
			// fetch the bitmap from
			// the cache
			if (mImageCache != null && !isCancelled()
					&& getAttachedImageView() != null && !mExitTasksEarly) {
				bitmap = mImageCache.getBitmapFromDiskCache(dataString);
			}

			// If the bitmap was not found in the cache and this task has not
			// been cancelled by
			// another thread and the ImageView that was originally bound to
			// this task is still
			// bound back to this task and our "exit early" flag is not set,
			// then call the main
			// process method (as implemented by a subclass)
			if (bitmap == null && !isCancelled()
					&& getAttachedImageView() != null && !mExitTasksEarly) {
				bitmap = processBitmap(mBitmapTaskItem, this);
			}

			// If the bitmap was processed and the image cache is available,
			// then add the processed
			// bitmap to the cache for future use. Note we don't check if the
			// task was cancelled
			// here, if it was, and the thread is still running, we may as well
			// add the processed
			// bitmap to our cache as it might be used again in the future
			if (bitmap != null) {
				// 不使用BitmapDrawable，会导致图片加载错位
				// if (Utils.hasHoneycomb()) {
				// // Running on Honeycomb or newer, so wrap in a standard
				// // BitmapDrawable
				// drawable = new BitmapDrawable(mResources, bitmap);
				// } else {
				// Running on Gingerbread or older, so wrap in a
				// RecyclingBitmapDrawable
				// which will recycle automagically
				drawable = new RecyclingBitmapDrawable(mResources, bitmap);
				// }

				if (mImageCache != null) {
					mImageCache.addBitmapToCache(dataString, drawable);
				}
			}

			if (BuildConfig.DEBUG) {
				Log.d(TAG, "doInBackground - finished work");
			}

			return drawable;
		}

		/**
		 * Once the image is processed, associates it to the imageView
		 */
		@Override
		protected void onPostExecute(BitmapDrawable value) {
			// if cancel was called on this task or the "exit early" flag is set
			// then we're done
			if (isCancelled() || mExitTasksEarly) {
				value = null;
			}

			final ImageView imageView = getAttachedImageView();
			if (value != null && imageView != null) {
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "onPostExecute - setting bitmap");
				}
				setImageDrawable(imageView, value);
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (isCancelled() || mExitTasksEarly) {
				return;
			}

			final ImageView imageView = getAttachedImageView();
			if (values != null && imageView != null
					&& imageView instanceof ProgressImageView) {
				((ProgressImageView) imageView).setProgress(values[0]);
			}
		}

		@Override
		public void updateProgress(int progress) {
			publishProgress(progress);
		}

		@Override
		protected void onCancelled(BitmapDrawable value) {
			super.onCancelled(value);
			synchronized (mPauseWorkLock) {
				mPauseWorkLock.notifyAll();
			}
		}

		/**
		 * Returns the ImageView associated with this task as long as the
		 * ImageView's task still points to this task as well. Returns null
		 * otherwise.
		 */
		private ImageView getAttachedImageView() {
			final ImageView imageView = imageViewReference.get();
			final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

			if (this == bitmapWorkerTask) {
				return imageView;
			}

			return null;
		}
	}

	/**
	 * A custom Drawable that will be attached to the imageView while the work
	 * is in progress. Contains a reference to the actual worker task, so that
	 * it can be stopped if a new binding is required, and makes sure that only
	 * the last started worker process can bind its result, independently of the
	 * finish order.
	 */
	private static class AsyncBitmapDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncBitmapDrawable(Resources res, Bitmap bitmap,
				BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
					bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	/**
	 * 异步请求加载图片的nine patch drawable
	 * 
	 * @author Calvin
	 * 
	 */
	private static class AsyncNinePatchDrawable extends NinePatchDrawable {

		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}

		public AsyncNinePatchDrawable(Resources res, Bitmap bitmap,
				byte[] chunk, Rect padding, String srcName,
				BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap, chunk, padding, srcName);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
					bitmapWorkerTask);
		}

		/**
		 * 计算NinePatchChunk的工具类
		 * 
		 * @author Calvin
		 * 
		 */
		static class NinePatchChunk {

			public static final int NO_COLOR = 0x00000001;
			public static final int TRANSPARENT_COLOR = 0x00000000;

			public Rect mPaddings = new Rect();

			public int mDivX[];
			public int mDivY[];
			public int mColor[];

			private static void readIntArray(int[] data, ByteBuffer buffer) {
				for (int i = 0, n = data.length; i < n; ++i) {
					data[i] = buffer.getInt();
				}
			}

			private static void checkDivCount(int length) {
				if (length == 0 || (length & 0x01) != 0) {
					throw new RuntimeException("invalid nine-patch: " + length);
				}
			}

			public static NinePatchChunk deserialize(byte[] data) {
				ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(
						ByteOrder.nativeOrder());

				byte wasSerialized = byteBuffer.get();
				if (wasSerialized == 0)
					return null;

				NinePatchChunk chunk = new NinePatchChunk();
				chunk.mDivX = new int[byteBuffer.get()];
				chunk.mDivY = new int[byteBuffer.get()];
				chunk.mColor = new int[byteBuffer.get()];

				checkDivCount(chunk.mDivX.length);
				checkDivCount(chunk.mDivY.length);

				// skip 8 bytes
				byteBuffer.getInt();
				byteBuffer.getInt();

				chunk.mPaddings.left = byteBuffer.getInt();
				chunk.mPaddings.right = byteBuffer.getInt();
				chunk.mPaddings.top = byteBuffer.getInt();
				chunk.mPaddings.bottom = byteBuffer.getInt();

				// skip 4 bytes
				byteBuffer.getInt();

				readIntArray(chunk.mDivX, byteBuffer);
				readIntArray(chunk.mDivY, byteBuffer);
				readIntArray(chunk.mColor, byteBuffer);

				return chunk;
			}
		}
	}

	/**
	 * Called when the processing is complete and the final drawable should be
	 * set on the ImageView.
	 * 
	 * @param imageView
	 * @param drawable
	 */
	private void setImageDrawable(ImageView imageView, Drawable drawable) {
		// Bug fix by XuWei 2013-09-09
		// 若直接调用缓存中Drawable则会产生图片大小尺寸异常的bug，复用了其他View缩放好的Drawable，故重新生成新的Drawable
		Drawable copyDrawable = new BitmapDrawable(mResources,
				((BitmapDrawable) drawable).getBitmap());

		if (mFadeInBitmap) {
			// Transition drawable with a transparent drawable and the final
			// drawable
			final TransitionDrawable td = new TransitionDrawable(
					new Drawable[] {
							new ColorDrawable(android.R.color.transparent),
							copyDrawable });
			// Set background to loading bitmap
			imageView.setImageDrawable(new BitmapDrawable(mResources,
					mLoadingBitmap));

			imageView.setImageDrawable(td);
			td.startTransition(FADE_IN_TIME);
		} else {
			imageView.setImageDrawable(copyDrawable);
		}
	}

	/**
	 * Pause any ongoing background work. This can be used as a temporary
	 * measure to improve performance. For example background work could be
	 * paused when a ListView or GridView is being scrolled using a
	 * {@link android.widget.AbsListView.OnScrollListener} to keep scrolling
	 * smooth.
	 * <p>
	 * If work is paused, be sure setPauseWork(false) is called again before
	 * your fragment or activity is destroyed (for example during
	 * {@link android.app.Activity#onPause()}), or there is a risk the
	 * background thread will never finish.
	 */
	public void setPauseWork(boolean pauseWork) {
		synchronized (mPauseWorkLock) {
			mPauseWork = pauseWork;
			if (!mPauseWork) {
				mPauseWorkLock.notifyAll();
			}
		}
	}

	/**
	 * 缓存异步处理类
	 * 
	 * @author Calvin
	 * 
	 */
	protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

		@Override
		protected Void doInBackground(Object... params) {
			switch ((Integer) params[0]) {
			case MESSAGE_CLEAR:
				clearCacheInternal();
				break;
			case MESSAGE_INIT_DISK_CACHE:
				initDiskCacheInternal();
				break;
			case MESSAGE_FLUSH:
				flushCacheInternal();
				break;
			case MESSAGE_CLOSE:
				closeCacheInternal();
				break;
			case MESSAGE_CLEAR_MEMORY:
				clearMemoryCacheInternal();
				break;
			}
			return null;
		}
	}

	/**
	 * 初始化缓存
	 */
	protected void initDiskCacheInternal() {
		if (mImageCache != null) {
			mImageCache.initDiskCache();
		}
	}

	/**
	 * 清除缓存，其中包含硬盘缓存以及内存缓存
	 */
	protected void clearCacheInternal() {
		if (mImageCache != null) {
			mImageCache.clearCache();
		}
	}

	/**
	 * 清除内存缓存
	 */
	protected void clearMemoryCacheInternal() {
		if (mImageCache != null) {
			mImageCache.clearMemoryCache();
		}
	}

	/**
	 * 清除当前缓冲区内的硬盘缓存
	 */
	protected void flushCacheInternal() {
		if (mImageCache != null) {
			mImageCache.flush();
		}
	}

	/**
	 * 关闭缓存
	 */
	protected void closeCacheInternal() {
		if (mImageCache != null) {
			mImageCache.close();
			mImageCache = null;
		}
	}

	public void clearCache() {
		new CacheAsyncTask().execute(MESSAGE_CLEAR);
	}

	public void flushCache() {
		new CacheAsyncTask().execute(MESSAGE_FLUSH);
	}

	public void closeCache() {
		new CacheAsyncTask().execute(MESSAGE_CLOSE);
	}

	public void clearMemoryCache() {
		new CacheAsyncTask().execute(MESSAGE_CLEAR_MEMORY);
	}

	/*
	 * 2013-5-28 加入传入回调获取图片或图片地址uri的请求方法
	 */
	/**
	 * 用于获取非在加载ImageView上的Bitmap或实体文件Uri。其中默认构造一个 {@link BitmapTaskItem}
	 * ，子类如需实现具体的处理，可重写此方法，传入不同的 {@link BitmapTaskItem}从而在
	 * {@link #processBitmap(Object)}方法体中进行具体实现
	 * 
	 * @param item
	 *            The loadtask of the image to download.
	 * @param callback
	 *            The BitmapWorkCallbackTaskContainer to sync with the bitmap.
	 * @see ImageFilepathCallback
	 * @see ImageBitmapCallback
	 */
	public void loadImage(Object data, BitmapWorkCallbackTaskContainer callback) {
		loadImageInternal(new BitmapTaskItem(data), callback);
	}

	/**
	 * 用于获取非在加载ImageView上的Bitmap或实体文件Uri
	 * 
	 * @param item
	 *            The loadtask of the image to download.
	 * @param callback
	 *            The BitmapWorkCallbackTaskContainer to sync with the bitmap.
	 * @see ImageFilepathCallback
	 * @see ImageBitmapCallback
	 */
	final protected void loadImageInternal(BitmapTaskItem item,
			BitmapWorkCallbackTaskContainer callback) {
		if (item == null || item.mDataSource == null || callback == null) {
			return;
		}

		// 字符串的非空排查
		if (item.mDataSource instanceof String
				&& TextUtils.isEmpty((CharSequence) item.mDataSource)) {
			return;
		}
		// resId的非空排查s
		if (item.mDataSource instanceof Integer
				&& (Integer) item.mDataSource == 0) {
			return;
		}

		BitmapDrawable value = null;

		if (mImageCache != null) {
			value = mImageCache.getBitmapFromMemCache(String
					.valueOf(item.mDataSource));
		} else if (callback instanceof ImageFilepathCallback) {
			Log.e(TAG,
					"ImageFilepathCallback need a ImageCache to get the image filepath.");
			return;
		}

		if (value != null && callback instanceof ImageWorkerDrawableCallback) {
			// Bitmap found in memory cache
			// Bug fix by XuWei 2013-09-09
			// 若直接调用缓存中Drawable则会产生图片大小尺寸异常的bug，复用了其他View缩放好的Drawable，故重新生成新的Drawable
			Drawable copyDrawable = new BitmapDrawable(mResources,
					value.getBitmap());
			((ImageWorkerDrawableCallback) callback)
					.getImageDrawable(copyDrawable);
		} else if (cancelPotentialWork(item.mDataSource, callback)) {
			final BitmapWorkerCallbackTask task = new BitmapWorkerCallbackTask(
					callback);
			callback.setBitmapWorkerCallbackTask(task);

			// NOTE: This uses a custom version of AsyncTask that has been
			// pulled from the
			// framework and slightly modified. Refer to the docs at the top of
			// the class
			// for more info on what was changed.
			task.executeOnExecutor(mExecutor, item);
		}

	}

	/**
	 * Returns true if the current work has been canceled or if there was no
	 * work in progress on this callback. Returns false if the work in progress
	 * deals with the same data. The work is not stopped in that case.
	 */
	public static boolean cancelPotentialWork(Object data,
			BitmapWorkCallbackTaskContainer callback) {
		final BitmapWorkerCallbackTask bitmapWorkerTask = callback
				.getBitmapWorkerCallbackTask();

		if (bitmapWorkerTask != null) {
			final BitmapTaskItem bitmapTaskItem = bitmapWorkerTask.mBitmapTaskItem;
			if (bitmapTaskItem == null || bitmapTaskItem.mDataSource == null
					|| !bitmapTaskItem.mDataSource.equals(data)) {
				bitmapWorkerTask.cancel(true);
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "cancelPotentialWork - cancelled work for "
							+ data);
				}
			} else {
				// The same work is already in progress.
				return false;
			}
		}
		return true;
	}

	/**
	 * 处理带有回调接口的图片请求
	 * 
	 * @author Calvin
	 * 
	 */
	private class BitmapWorkerCallbackTask extends
			AsyncTask<BitmapTaskItem, Integer, Object[]> implements
			OnProcessProgressUpdate {

		private BitmapTaskItem mBitmapTaskItem;
		private BitmapWorkCallbackTaskContainer callback;

		public BitmapWorkerCallbackTask(BitmapWorkCallbackTaskContainer callback) {
			this.callback = callback;
		}

		/**
		 * Background processing.
		 */
		@Override
		protected Object[] doInBackground(BitmapTaskItem... params) {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "doInBackground - starting work");
			}

			mBitmapTaskItem = params[0];
			final String dataString = String
					.valueOf(mBitmapTaskItem.mDataSource);
			Bitmap bitmap = null;
			BitmapDrawable drawable = null;
			String filepath = null;

			// Wait here if work is paused and the task is not cancelled
			synchronized (mPauseWorkLock) {
				while (mPauseWork && !isCancelled()) {
					try {
						mPauseWorkLock.wait();
					} catch (InterruptedException e) {
					}
				}
			}

			// If the image cache is available and this task has not been
			// cancelled by another
			// thread and the ImageView that was originally bound to this task
			// is still bound back
			// to this task and our "exit early" flag is not set then try and
			// fetch the bitmap from
			// the cache
			if (mImageCache != null && !isCancelled()
					&& getAttachedCallback() != null && !mExitTasksEarly) {
				bitmap = mImageCache.getBitmapFromDiskCache(dataString);
			}

			// If the bitmap was not found in the cache and this task has not
			// been cancelled by
			// another thread and the ImageView that was originally bound to
			// this task is still
			// bound back to this task and our "exit early" flag is not set,
			// then call the main
			// process method (as implemented by a subclass)
			if (bitmap == null && !isCancelled()
					&& getAttachedCallback() != null && !mExitTasksEarly) {
				bitmap = processBitmap(mBitmapTaskItem, this);
			}

			// If the bitmap was processed and the image cache is available,
			// then add the processed
			// bitmap to the cache for future use. Note we don't check if the
			// task was cancelled
			// here, if it was, and the thread is still running, we may as well
			// add the processed
			// bitmap to our cache as it might be used again in the future
			if (bitmap != null) {
				// 不使用BitmapDrawable，会导致图片加载错位
				// if (Utils.hasHoneycomb()) {
				// // Running on Honeycomb or newer, so wrap in a standard
				// // BitmapDrawable
				// drawable = new BitmapDrawable(mResources, bitmap);
				// } else {
				// Running on Gingerbread or older, so wrap in a
				// RecyclingBitmapDrawable
				// which will recycle automagically
				drawable = new RecyclingBitmapDrawable(mResources, bitmap);
				// }

				if (mImageCache != null) {
					mImageCache.addBitmapToCache(dataString, drawable);
				}

				// 若bitmap可从缓存中获取到，则取出缓存文件路径
				if (bitmap != null && mImageCache != null) {
					try {
						filepath = Uri
								.fromFile(
										new File(
												mImageCache
														.getBitmapFilepathFromDiskCache(dataString)))
								.toString();
					} catch (Exception e) {
						Log.e(TAG, e);
					}
				}
			}

			if (BuildConfig.DEBUG) {
				Log.d(TAG, "doInBackground - finished work");
			}

			return new Object[] { drawable, filepath };
		}

		/**
		 * Once the image is processed, associates it to the callback
		 */
		@Override
		protected void onPostExecute(Object[] values) {
			// if cancel was called on this task or the "exit early" flag is set
			// then we're done
			if (isCancelled() || mExitTasksEarly) {
				values[0] = null;
				values[1] = null;
				values = null;
			}

			final BitmapWorkCallbackTaskContainer callback = getAttachedCallback();
			if (values != null && callback != null) {
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "onPostExecute - setting bitmap");
				}

				// 防止在js替换img src参数时因为加载顺序问题，而导致的js页面代码刷新图片不完整的现象发生
				synchronized (mCallbackSyncLock) {
					if (callback instanceof ImageFilepathCallback) {
						if (values[1] != null) {
							((ImageFilepathCallback) callback)
									.getImageFilePath((String) values[1]);
						}
					} else if (callback instanceof ImageDrawableCallback) {
						BitmapDrawable result = (BitmapDrawable) values[0];
						if (result != null) {
							((ImageDrawableCallback) callback)
									.getImageDrawable(result);
						}
					}
				}
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			final BitmapWorkCallbackTaskContainer callback = getAttachedCallback();
			if (values != null && callback != null) {
				callback.updateProgress(values[0]);
			}
		}

		@Override
		public void updateProgress(int progress) {
			publishProgress(progress);
		}

		@Override
		protected void onCancelled(Object[] value) {
			super.onCancelled(value);
			synchronized (mPauseWorkLock) {
				mPauseWorkLock.notifyAll();
			}
		}

		/**
		 * Returns the Callback associated with this task as long as the
		 * Callback's task still points to this task as well. Returns null
		 * otherwise.
		 */
		private BitmapWorkCallbackTaskContainer getAttachedCallback() {
			if (callback == null) {
				return null;
			}
			final BitmapWorkerCallbackTask bitmapWorkerTask = callback
					.getBitmapWorkerCallbackTask();

			if (this == bitmapWorkerTask) {
				return callback;
			}

			return null;
		}
	}

	/**
	 * 传入loadImage方法中的基类，主要用于装载BitmapWorkerCallbackTask
	 * 
	 * @author Calvin
	 * 
	 */
	protected static abstract class BitmapWorkCallbackTaskContainer {
		/** 软引用实例 */
		private WeakReference<BitmapWorkerCallbackTask> mTaskWeakReference;

		/**
		 * 绑定一个task时采用软引用的方式，避免oom问题
		 * 
		 * @param task
		 */
		private void setBitmapWorkerCallbackTask(BitmapWorkerCallbackTask task) {
			mTaskWeakReference = new WeakReference<BitmapWorkerCallbackTask>(
					task);
		}

		private BitmapWorkerCallbackTask getBitmapWorkerCallbackTask() {
			if (mTaskWeakReference == null) {
				return null;
			}
			return mTaskWeakReference.get();
		}

		/**
		 * 更新进度
		 * 
		 * @param progress
		 */
		public void updateProgress(int progress) {
			// override it if need
		}
	}

	/**
	 * 请求一个图片，在回调中获取图片的实体文件Uri
	 * 
	 * @author Calvin
	 * 
	 */
	public static abstract class ImageFilepathCallback extends
			BitmapWorkCallbackTaskContainer implements
			ImageWorkerBitmapPathCallback {
	}

	/**
	 * 请求一个图片，在回调中获取图片bitmap
	 * 
	 * @author Calvin
	 * 
	 */
	public static abstract class ImageDrawableCallback extends
			BitmapWorkCallbackTaskContainer implements
			ImageWorkerDrawableCallback {
	}

	/**
	 * 获取图片Uri的回调接口
	 * 
	 * @author Calvin
	 * 
	 */
	private interface ImageWorkerBitmapPathCallback {
		public void getImageFilePath(String filePath);
	}

	/**
	 * 获取图片drawable的回调接口
	 * 
	 * @author Calvin
	 * 
	 */
	private interface ImageWorkerDrawableCallback {
		public void getImageDrawable(Drawable drawable);
	}

	/*
	 * 2013-7-11日加入图片加载任务item，用于提供子类更好的扩展
	 */
	protected class BitmapTaskItem {
		/** 图片数据源，缓存标识 */
		protected Object mDataSource;

		/**
		 * 构造
		 * 
		 * @param dataSource
		 */
		protected BitmapTaskItem(Object dataSource) {
			mDataSource = dataSource;
		}
	}

	/*
	 * 2013-7-17日加入图片加载任务进度回调，由于AsyncTask的onProgressUpdate方法为protected类型，
	 * 所以需要在processBitmap方法中传入此回调
	 */
	protected interface OnProcessProgressUpdate {

		/**
		 * 更新进度
		 * 
		 * @param progress
		 */
		void updateProgress(int progress);
	}
}
