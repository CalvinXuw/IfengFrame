package com.ifeng.util.model;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.ifeng.BaseApplicaion;
import com.ifeng.util.logging.Log;

/**
 * Model的基类，所有的Model本质上都从它继承，重写{@link #process()}方法将耗时操作在此实现，并借由
 * {@link #executeAsyncTask()}、{@link #executeSyncTask()}、
 * {@link #scheduleAsyncTask(int)}、{@link #scheduleSyncTask(int)}方法进行同步或异步的处理，从
 * {@link OnModelProcessListener}监听请求结果状态
 * 
 * @author xuwei
 * 
 */
public abstract class AbstractModel {

	/** log tag. */
	protected final String TAG = getClass().getSimpleName();
	/** if enabled, logcat will output the log. */
	protected static final boolean DEBUG = true & BaseApplicaion.DEBUG;

	/** context */
	protected Context mContext;
	/** 主线程Handler */
	protected Handler mHandler;

	/** 线程优先级 */
	private int mPriority = Process.THREAD_PRIORITY_DEFAULT;
	/** 线程池 */
	private static final ScheduledExecutorService THREAD_POOL = Executors
			.newScheduledThreadPool(5);

	/** 当前model的状态 */
	protected ModelProcessState mProcessState;
	/** 处理结果回调Listener */
	protected OnModelProcessListener mOnModelProcessListener;

	/** 是否任务已经取消 */
	private boolean mIsCanceled;
	/** 是否任务已经暂停 */
	private boolean mIsPaused;
	/** 任务暂停锁 */
	private Object mPauseLock = new Object();

	/**
	 * 构造
	 * 
	 * @param context
	 * @param listener
	 *            请求回调
	 */
	public AbstractModel(Context context, OnModelProcessListener listener) {
		mContext = context.getApplicationContext();
		mOnModelProcessListener = listener;

		initInternal();
	}

	/**
	 * 初始化model
	 */
	private void initInternal() {
		if (DEBUG) {
			Log.d(TAG, "model has init");
		}

		// 请求数据时，要求回调
		if (mOnModelProcessListener != null) {
			mHandler = new Handler(Looper.getMainLooper());
			init();
		}
	}

	/**
	 * 提供子类进行初始化
	 */
	protected abstract void init();

	/**
	 * 执行异步任务
	 */
	public void executeAsyncTask() {
		scheduleAsyncTask(0);
	}

	/**
	 * 执行同步任务，在ui线程中
	 */
	public void executeSyncTask() {
		scheduleSyncTask(0);
	}

	/**
	 * 执行计划异步任务
	 * 
	 * @param milliseconds
	 */
	public void scheduleAsyncTask(int milliseconds) {
		if (!processCanStart()) {
			return;
		}

		THREAD_POOL.schedule(new Runnable() {

			@Override
			public void run() {
				Process.setThreadPriority(mPriority);
				process();
			}
		}, milliseconds, TimeUnit.MILLISECONDS);
	}

	/**
	 * 执行计划同步任务
	 * 
	 * @param milliseconds
	 */
	public void scheduleSyncTask(int milliseconds) {
		if (!processCanStart()) {
			return;
		}

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				process();
			}
		}, milliseconds);
	}

	/**
	 * 执行耗时操作
	 */
	protected abstract void process();

	/**
	 * 更新进度
	 * 
	 * @param progress
	 */
	protected final void postProgress(final int progress) {
		if (mOnModelProcessListener == null) {
			return;
		}

		Runnable postProgressRunnable = new Runnable() {

			@Override
			public void run() {
				mOnModelProcessListener
						.onProgress(AbstractModel.this, progress);
			}
		};

		if (Thread.currentThread().getId() != Looper.getMainLooper()
				.getThread().getId()) {
			mHandler.post(postProgressRunnable);
		} else {
			postProgressRunnable.run();
		}
	}

	/**
	 * 处理成功
	 */
	protected void onSuccess() {
		// 此请求已经被Cancel
		if (!processCanMoveOn()) {
			mProcessState = ModelProcessState.CANCEL;
			return;
		}
		mProcessState = ModelProcessState.READY;

		if (DEBUG) {
			Log.d(TAG, "process successed");
		}

		// 回调
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mOnModelProcessListener.onSuccess(AbstractModel.this);
			}
		});
	}

	/**
	 * 处理失败
	 * 
	 * @param errorCode
	 */
	protected void onFailed(final int errorCode) {
		// 此请求已经被Cancel
		if (!processCanMoveOn()) {
			mProcessState = ModelProcessState.CANCEL;
			return;
		}
		mProcessState = ModelProcessState.FAILED;

		if (DEBUG) {
			Log.d(TAG, "process failed");
		}

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mOnModelProcessListener.onFailed(AbstractModel.this, errorCode);
			}
		});
	}

	/**
	 * 是否已经被取消
	 * 
	 * @return the mIsCancel
	 */
	public boolean isCanceled() {
		return mIsCanceled;
	}

	/**
	 * 取消
	 */
	public void cancel() {
		if (DEBUG) {
			Log.d(TAG, "model process canceled");
		}

		mIsCanceled = true;

		synchronized (mPauseLock) {
			mPauseLock.notifyAll();
		}
	}

	/**
	 * 设置暂停
	 * 
	 * @param isPause
	 */
	public void setPause(boolean isPause) {
		synchronized (mPauseLock) {
			mIsPaused = isPause;
			if (!mIsPaused) {
				mPauseLock.notifyAll();
			}
		}
	}

	/**
	 * 检查当前model状态，是否可以开始处理一个任务
	 * 
	 * @return
	 */
	protected boolean processCanStart() {
		if (mProcessState == ModelProcessState.RUNNING) {
			if (DEBUG) {
				Log.d(TAG, "already has a processing task");
			}
			return false;
		}

		if (mProcessState == ModelProcessState.CANCEL) {
			if (DEBUG) {
				Log.d(TAG, "task already canceled");
			}
			return false;
		}

		return true;
	}

	/**
	 * 检查当前model状态，是否为暂停或者已经被取消
	 * 
	 * @return
	 */
	protected boolean processCanMoveOn() {
		// Wait here if work is paused and return the task is cancelled
		synchronized (mPauseLock) {
			while (mIsPaused && !mIsCanceled) {
				try {
					mPauseLock.wait();
				} catch (InterruptedException e) {
				}
			}
		}

		return !mIsCanceled;
	}

	/**
	 * @return the priority
	 */
	public int getPriority() {
		return mPriority;
	}

	/**
	 * @param priority
	 *            the priority to set
	 */
	public void setPriority(int priority) {
		this.mPriority = priority;
	}

	/**
	 * 数据获取结果的Listener
	 * 
	 * @author xuwei
	 * 
	 */
	public interface OnModelProcessListener {

		/**
		 * 处理成功
		 * 
		 * @param model
		 *            model
		 */
		void onSuccess(AbstractModel model);

		/**
		 * 处理失败
		 * 
		 * @param model
		 *            model
		 * @param errorCode
		 *            {@link IModelErrorCode}错误码
		 */
		void onFailed(AbstractModel model, int errorCode);

		/**
		 * 处理进度回调
		 * 
		 * @param model
		 * @param progress
		 */
		void onProgress(AbstractModel model, int progress);
	}

	/**
	 * 当前model的状态参数，就绪、处理中、失败、已取消
	 * 
	 * @author Calvin
	 * 
	 */
	public enum ModelProcessState {
		READY, RUNNING, FAILED, CANCEL
	}

	/**
	 * 需要子类重写父类方法，用于提示的异常
	 * 
	 * @author Calvin
	 * 
	 */
	protected class NeedOverrideException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -5969876713354283460L;

		public NeedOverrideException(String string) {
			super(string);
		}
	}
}
