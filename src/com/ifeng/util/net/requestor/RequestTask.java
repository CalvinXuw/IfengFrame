package com.ifeng.util.net.requestor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Process;

import com.ifeng.BaseApplicaion;
import com.ifeng.util.logging.Log;

public abstract class RequestTask implements Runnable {
	/** log tag. */
	private static final String TAG = RequestTask.class.getSimpleName();

	/** if enabled, logcat will output the log. */
	protected static final boolean DEBUG = true & BaseApplicaion.DEBUG;

	/**
	 * 线程池
	 */
	private static final ExecutorService THREAD_POOL = Executors
			.newFixedThreadPool(5);

	/**
	 * 任务是否已经删除
	 */
	private AtomicBoolean mIsCancel = new AtomicBoolean();

	/**
	 * 线程优先级
	 */
	private int mPriority;

	/**
	 * 请求结果反馈
	 */
	protected OnRequestTaskListener mOnRequestTaskListener;

	/**
	 * 构造函数
	 * 
	 * @param listener
	 *            回调Listener
	 */
	public RequestTask(OnRequestTaskListener listener) {
		this(Process.THREAD_PRIORITY_DEFAULT, listener);
	}

	/**
	 * 构造函数
	 * 
	 * @param priority
	 *            线程优先级
	 * @param listener
	 *            回调Listener
	 */
	public RequestTask(int priority, OnRequestTaskListener listener) {
		mPriority = priority;
		mOnRequestTaskListener = listener;
	}

	@Override
	public final void run() {
		if (mIsCancel.get()) {
			// 请求已经撤销
			return;
		}

		// 线程优化级
		Process.setThreadPriority(mPriority);

		processRequest();
	}

	/**
	 * 子类实现耗时操作
	 */
	protected abstract void processRequest();

	/**
	 * 任务执行
	 */
	@Deprecated
	public void execute() {
		if (DEBUG) {
			Log.d(TAG,
					"---- prepare request time:" + System.currentTimeMillis());
		}
		THREAD_POOL.execute(this);
	}

	/**
	 * @return 是否已经撤销
	 */
	public boolean isCancel() {
		return mIsCancel.get();
	}

	/**
	 * 撤销任务执行
	 */
	public void cancel() {
		mIsCancel.set(true);
	}

	/**
	 * 获取数据结果的Listener
	 * 
	 * @author xuwei
	 * 
	 */
	protected interface OnRequestTaskListener {
		/**
		 * 请求成功
		 * 
		 * @param result
		 *            获取到的String数据
		 */
		void onSuccess(String result);

		/**
		 * 请求失败
		 * 
		 * @param errorCode
		 *            错误码
		 */
		void onFailed(int errorCode);

		/**
		 * 请求进度
		 */
		void onProgress();
	}
}
