package com.ifeng.util.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import com.ifeng.BaseApplicaion;

/**
 * 增強型holder，内容view控制器，用于处理header、footer、listviewitem等
 * 
 * @author xuwei
 * 
 */
public abstract class ViewController {
	/** tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug开关 */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;

	/** activity */
	private Activity mActivity;
	/** 生成器 */
	private LayoutInflater mInflater;
	/** 内容view */
	private View mContentView;

	/**
	 * 构造
	 * 
	 * @param activity
	 */
	public ViewController(Activity activity) {
		mActivity = activity;
		mInflater = activity.getLayoutInflater();
		mContentView = init();
	}

	/** 初始化界面 */
	protected abstract View init();

	/**
	 * get activity
	 * 
	 * @return
	 */
	protected final Activity getActivity() {
		return mActivity;
	}

	/**
	 * get layoutinflater
	 * 
	 * @return
	 */
	protected final LayoutInflater getLayoutInflater() {
		return mInflater;
	}

	/**
	 * 获取内容view
	 * 
	 * @return
	 */
	public View getContentView() {
		return mContentView;
	}
}
