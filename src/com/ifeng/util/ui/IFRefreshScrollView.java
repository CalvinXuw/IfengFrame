package com.ifeng.util.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import com.ifeng.BaseApplicaion;
import com.ifeng.util.Utility;
import com.ifeng.util.imagecache.Utils;
import com.ifeng.util.logging.Log;
import com.ifeng.util.ui.IFRefreshViewLayout.IRefreshScrollView;
import com.ifeng.util.ui.IFRefreshViewLayout.OnScrollToBottomListener;

/**
 * 实现了{@link IFRefreshScrollView} 的接口，若需实现下拉刷新、上拉加载、滚动加载等功能则在
 * {@link IFRefreshViewLayout#setContentView(IFRefreshViewLayout.IRefreshView)}
 * 方法中传入本类实例即可
 * 
 * @author Calvin
 * 
 */
public class IFRefreshScrollView extends ScrollView implements
		IRefreshScrollView {

	/** Tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug开关 */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;

	/** 滚动监听回调 */
	private OnScrollToBottomListener mOnScrollToBottomListener;

	/**
	 * 构造
	 * 
	 * @param context
	 */
	public IFRefreshScrollView(Context context) {
		super(context);
		init();
	}

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public IFRefreshScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * 初始化
	 */
	@TargetApi(9)
	private void init() {
		if (Utils.hasGingerbread()) {
			setOverScrollMode(View.OVER_SCROLL_NEVER);
		}
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		if (mOnScrollToBottomListener != null) {
			try {
				// 默认为小余50dip的高度条件下触发事件
				if (getChildAt(0).getMeasuredHeight() - getScrollY()
						- getMeasuredHeight() < Utility
						.getDensity(getContext()) * 50) {
					mOnScrollToBottomListener.onScrollToBottom();
				}
			} catch (Exception e) {
				Log.e(TAG, "caught unknow exception");
				Log.e(TAG, e);
			}
		}
		super.onScrollChanged(l, t, oldl, oldt);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		try {
			return super.onInterceptTouchEvent(ev);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		try {
			return super.onTouchEvent(ev);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean isReachTheTop() {
		return getScrollY() == 0;
	}

	@Override
	public boolean isReachTheBottom() {
		try {
			return getScrollY() + getMeasuredHeight() == getChildAt(0)
					.getMeasuredHeight();
		} catch (Exception e) {
			Log.e(TAG, "caught unknow exception");
			Log.e(TAG, e);
		}
		return false;
	}

	@Override
	public void setOnScrollToBottomListener(OnScrollToBottomListener listener) {
		mOnScrollToBottomListener = listener;
	}

	@Override
	public void scrollToTop(boolean isAnim) {
		scrollTo(0, 0);
	}

	@Override
	public void scrollToBottom(boolean isAnim) {
		scrollTo(0, getChildAt(0).getMeasuredHeight());
	}
}
