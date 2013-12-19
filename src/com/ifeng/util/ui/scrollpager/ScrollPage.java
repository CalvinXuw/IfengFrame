package com.ifeng.util.ui.scrollpager;

import android.view.View;

/**
 * 滑动分页单页单元
 * 
 * @author xuwei
 * 
 */
public abstract class ScrollPage {

	/**
	 * 分页状态
	 * 
	 * @see #RUNNING 活动状态
	 * @see #PAUSE 暂停状态
	 * @see #UNINIT 尚未初始化
	 * 
	 * @author xuwei
	 * 
	 */
	private enum State {
		RUNNING, PAUSE, UNINIT
	}

	/** 当前分页状态 */
	private State mState = State.UNINIT;
	/** 分页装载的View */
	private View mView;

	/** view的生成创建，不建议在onCreate中进行耗时操作或进行网络初始化操作 */
	public abstract void onCreate();

	/** 当页面首次介入active状态的时候进行调用 */
	public abstract void onStart();

	/** 当页面由pause状态恢复到active状态时被调用 */
	public abstract void onResume();

	/** 当页面脱离active状态时被调用 */
	public abstract void onPause();

	/** 页面即将被销毁时被调用 */
	public abstract void onDestory();

	/** 获取页面装载的View */
	public View getView() {
		return mView;
	}

	/** 设置页面装载的View */
	public void setView(View view) {
		mView = view;
	}

	/**
	 * 唤起页面进入active状态
	 */
	protected final void start() {
		if (mState == State.UNINIT) {
			mState = State.RUNNING;
			onStart();
		} else if (mState == State.PAUSE) {
			mState = State.RUNNING;
			onResume();
		}
	}

	/**
	 * 暂挂页面进入pause状态
	 */
	protected final void pause() {
		if (mState == State.RUNNING) {
			mState = State.PAUSE;
			onPause();
		}
	}
}
