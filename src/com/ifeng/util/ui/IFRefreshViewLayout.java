package com.ifeng.util.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ifeng.BaseApplicaion;
import com.ifeng.android.R;
import com.ifeng.util.Utility;
import com.ifeng.util.logging.Log;
import com.ifeng.util.ui.IFRefreshViewLayout.IRefreshView;
import com.ifeng.util.ui.IIFAnimation.IFAnimation;

/**
 * 封装了下拉刷新、上拉加载、滚动加载实现的ViewGroup
 * 
 * @author Calvin
 * 
 */
public class IFRefreshViewLayout<T extends IRefreshView> extends RelativeLayout
		implements ISelfDispatchEventDelegate {

	/** Tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug开关 */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;
	/** content view id */
	private static final int ID_CONTENT_VIEW = 10086;

	/** 填充的内容控件 */
	private T mContentView;
	/** 是否主动分配touch事件 */
	private boolean mIsEnableDispatch;

	/*
	 * 下拉刷新配置
	 */
	/** 是否允许下拉刷新 */
	private boolean mIsPullDownRefreshEnable;
	/** 下拉刷新View */
	private BasePullDownRefreshView mPullDownView;
	/** 当前下拉刷新状态 */
	private PullDownRefreshState mPullDownRefreshState = PullDownRefreshState.DONE;
	/** 下拉刷新事件触发监听 */
	private OnPullDownRefreshListener mPullDownRefreshListener;
	/** 是否已经捕获到下拉刷新事件 */
	private boolean mIsPullDownEventDetected;
	/** 下拉刷新事件触摸起点记录 */
	private int mPullDownEventStartY;

	/*
	 * 上拉加载配置
	 */
	/** 是否允许上拉加载 */
	private boolean mIsPullUpRefreshEnable;
	/** 上拉加载View */
	private BasePullUpRefreshView mPullUpView;
	/** 当前上拉加载状态 */
	private PullUpRefreshState mPullUpRefreshState = PullUpRefreshState.DONE;
	/** 上拉加载事件触发监听 */
	private OnPullUpRefreshListener mPullUpRefreshListener;
	/** 是否已经捕获到上拉加载事件 */
	private boolean mIsPullUpEventDetected;
	/** 上拉加载事件触摸起点记录 */
	private int mPullUpEventStartY;

	/*
	 * 滚动加载配置
	 */
	/** 是否允许滑动加载 */
	private boolean mIsScrollRefreshEnable;
	/** 滚动加载View */
	private BaseScrollRefreshView mScrollRefreshView;
	/** 当前滚动加载状态 */
	private ScrollRefreshState mScrollRefreshState = ScrollRefreshState.DONE;
	/** 滚动加载事件触发监听 */
	private OnScrollRefreshListener mScrollRefreshListener;
	/** 滚动加载触摸点记录 */
	private float mScrollTouchEventRecordY = Float.MAX_VALUE;

	/**
	 * 构造
	 * 
	 * @param context
	 */
	public IFRefreshViewLayout(Context context) {
		super(context);
	}

	/**
	 * 构造
	 * 
	 * @param context
	 */
	public IFRefreshViewLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * 添加下拉刷新事件监听
	 * 
	 * @param pullDownRefreshListener
	 */
	public void setPullDownRefreshListener(
			OnPullDownRefreshListener pullDownRefreshListener) {
		setPullDownRefreshListener(pullDownRefreshListener,
				new BasePullDownRefreshView(getContext()));
	}

	/**
	 * 添加下拉刷新事件监听
	 * 
	 * @param pullDownRefreshListener
	 * @param pullDownRefreshView
	 */
	public void setPullDownRefreshListener(
			OnPullDownRefreshListener pullDownRefreshListener,
			BasePullDownRefreshView pullDownRefreshView) {
		mPullDownRefreshListener = pullDownRefreshListener;
		mIsPullDownRefreshEnable = true;
		mPullDownView = pullDownRefreshView;
		mPullDownRefreshState = PullDownRefreshState.DONE;
	}

	/**
	 * 添加上拉加载事件监听
	 * 
	 * @param pullUpRefreshListener
	 */
	public void setPullUpRefreshListener(
			OnPullUpRefreshListener pullUpRefreshListener) {
		setPullUpRefreshListener(pullUpRefreshListener,
				new BasePullUpRefreshView(getContext()));
	}

	/**
	 * 添加上拉加载事件监听
	 * 
	 * @param pullUpRefreshListener
	 * @param pullUpRefreshView
	 */
	public void setPullUpRefreshListener(
			OnPullUpRefreshListener pullUpRefreshListener,
			BasePullUpRefreshView pullUpRefreshView) {
		mPullUpRefreshListener = pullUpRefreshListener;
		mIsPullUpRefreshEnable = true;
		mPullUpView = pullUpRefreshView;
		mPullUpRefreshState = PullUpRefreshState.DONE;

		// 上拉加载和滚动加载唯一存在
		mIsScrollRefreshEnable = false;
	}

	/**
	 * 添加滑动加载事件监听
	 * 
	 * @param scrollRefreshListener
	 */
	public void setScrollRefreshListener(
			OnScrollRefreshListener scrollRefreshListener) {
		setScrollRefreshListener(scrollRefreshListener,
				new BaseScrollRefreshView(getContext()));
	}

	/**
	 * 添加滑动加载事件监听
	 * 
	 * @param scrollRefreshListener
	 * @param scrollRefreshView
	 */
	public void setScrollRefreshListener(
			OnScrollRefreshListener scrollRefreshListener,
			BaseScrollRefreshView scrollRefreshView) {
		mScrollRefreshListener = scrollRefreshListener;
		mIsScrollRefreshEnable = true;
		mScrollRefreshView = scrollRefreshView;
		mScrollRefreshState = ScrollRefreshState.DONE;

		// 上拉加载和滚动加载唯一存在
		mIsPullUpRefreshEnable = false;
	}

	/**
	 * 设置实际内容的View，注意调用顺序在
	 * {@link #setPullDownRefreshListener(OnPullDownRefreshListener)}、
	 * {@link #setPullUpRefreshListener(OnPullUpRefreshListener)}、
	 * {@link #setScrollRefreshListener(OnScrollRefreshListener)}之后。
	 * 
	 * @param mContentView
	 */
	public void setContentView(T mContentView) {
		this.mContentView = mContentView;

		init();
	}

	/**
	 * 进行一次下拉刷新
	 */
	public void forcePullDownRefresh() {
		if (getCurrentState() != IFRefreshViewState.NORMAL) {
			return;
		}

		mContentView.scrollToTop(true);
		onPullDownStateChanged(PullDownRefreshState.REFRESHING);
	}

	/**
	 * 进行一次上拉加载
	 */
	public void forcePullUpRefresh() {
		if (getCurrentState() != IFRefreshViewState.NORMAL) {
			return;
		}

		mContentView.scrollToBottom(true);
		onPullUpStateChanged(PullUpRefreshState.REFRESHING);
	}

	/**
	 * 通知下拉刷新完成
	 */
	public void onPullDownRefreshComplete() {
		onPullDownStateChanged(PullDownRefreshState.DONE);
		mPullDownView.notifyUpdateTime(System.currentTimeMillis());
		mContentView.scrollToTop(true);

		// 复位上拉以及滚动加载
		if (mIsScrollRefreshEnable) {
			onScrollRefreshStateChanged(ScrollRefreshState.DONE);
		}
		if (mIsPullUpRefreshEnable) {
			onPullUpStateChanged(PullUpRefreshState.DONE);
		}
	}

	/**
	 * 通知下拉刷新失败
	 */
	public void onPullDownRefreshFailed() {
		onPullDownStateChanged(PullDownRefreshState.DONE);
	}

	/**
	 * 通知下拉刷新无后续可加载项
	 */
	public void onPullDownRefreshNoMore() {
		onPullDownStateChanged(PullDownRefreshState.NOMORE);
	}

	/**
	 * 通知上拉加载完成
	 */
	public void onPullUpRefreshComplete() {
		onPullUpStateChanged(PullUpRefreshState.DONE);
		mPullUpView.notifyUpdateTime(System.currentTimeMillis());
	}

	/**
	 * 通知上拉加载失败
	 */
	public void onPullUpRefreshFailed() {
		onPullUpStateChanged(PullUpRefreshState.DONE);
	}

	/**
	 * 通知上拉加载无后续可加载项
	 */
	public void onPullUpRefreshNoMore() {
		onPullUpStateChanged(PullUpRefreshState.NOMORE);
	}

	/**
	 * 通知滚动加载完成
	 */
	public void onScrollRefreshComplete() {
		onScrollRefreshStateChanged(ScrollRefreshState.DONE);
		mScrollRefreshView.notifyUpdateTime(System.currentTimeMillis());
	}

	/**
	 * 通知滚动加载失败
	 */
	public void onScrollRefreshFail() {
		onScrollRefreshStateChanged(ScrollRefreshState.FAIL);
	}

	/**
	 * 通知滑动加载无后续可加载项
	 */
	public void onScrollRefreshNoMore() {
		onScrollRefreshStateChanged(ScrollRefreshState.NOMORE);
	}

	/**
	 * 获取当前RefreshView状态
	 * 
	 * @return
	 */
	public IFRefreshViewState getCurrentState() {
		if (mPullDownRefreshState == PullDownRefreshState.REFRESHING) {
			return IFRefreshViewState.WAITING_PULL_DOWN_REFRESH_RESULT;
		} else if (mPullUpRefreshState == PullUpRefreshState.REFRESHING) {
			return IFRefreshViewState.WAITING_PULL_UP_REFRESH_RESULT;
		} else if (mScrollRefreshState == ScrollRefreshState.REFRESHING) {
			return IFRefreshViewState.WAITING_SCROLLREFRESH_RESULT;
		}
		return IFRefreshViewState.NORMAL;
	}

	/**
	 * 初始化
	 */
	private void init() {
		View contentView = (View) mContentView;
		contentView.setId(ID_CONTENT_VIEW);
		contentView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		addView(contentView);

		// 若传入对象为ListView，则采取List添加Header、Footer的方法加入下拉上拉的View
		if (mContentView instanceof IRefreshListView) {
			if (mIsPullDownRefreshEnable) {
				((IRefreshListView) mContentView)
						.addHeaderRefreshView(mPullDownView.getContentView());
			}
			if (mIsPullUpRefreshEnable) {
				((IRefreshListView) mContentView)
						.addFooterRefreshView(mPullUpView.getContentView());
			}
			if (mIsScrollRefreshEnable) {
				((IRefreshListView) mContentView)
						.addFooterRefreshView(mScrollRefreshView
								.getContentView());
				mContentView.setOnScrollToBottomListener(mOnScrollListener);
			}
		} else if (mContentView instanceof IRefreshView) {
			if (mIsPullDownRefreshEnable) {
				View header = mPullDownView.getContentView();
				header.setId(ID_CONTENT_VIEW - 1);
				LayoutParams headerParams = new LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				header.setLayoutParams(headerParams);
				addView(header);

				RelativeLayout.LayoutParams contentParams = (LayoutParams) contentView
						.getLayoutParams();
				contentParams
						.addRule(RelativeLayout.BELOW, ID_CONTENT_VIEW - 1);
			}
			if (mIsPullUpRefreshEnable) {
				View footer = mPullUpView.getContentView();
				footer.setId(ID_CONTENT_VIEW + 1);
				LayoutParams footerParams = new LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				footerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				footer.setLayoutParams(footerParams);
				addView(footer);

				RelativeLayout.LayoutParams contentParams = (LayoutParams) contentView
						.getLayoutParams();
				contentParams
						.addRule(RelativeLayout.ABOVE, ID_CONTENT_VIEW + 1);
			}
			if (mIsScrollRefreshEnable
					&& mContentView instanceof IRefreshScrollView) {
				ScrollView scrollView = (ScrollView) contentView;

				LinearLayout layout = new LinearLayout(getContext());
				layout.setOrientation(LinearLayout.VERTICAL);

				View footer = mScrollRefreshView.getContentView();
				LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.FILL_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
				footer.setLayoutParams(footerParams);

				if (scrollView.getChildCount() > 0) {
					View child = scrollView.getChildAt(0);
					scrollView.removeAllViews();
					layout.addView(child);
					layout.addView(footer);
					scrollView.addView(layout);
				}
				mContentView.setOnScrollToBottomListener(mOnScrollListener);
			} else if (mIsScrollRefreshEnable) {
				throw new RuntimeException(
						"IRefreshView not support the ScrollRefresh");
			}
		}

		if (mIsPullDownRefreshEnable) {
			onPullDownStateChanged(PullDownRefreshState.DONE);
		}
		if (mIsPullUpRefreshEnable) {
			onPullUpStateChanged(PullUpRefreshState.DONE);
		}
		if (mIsScrollRefreshEnable) {
			onScrollRefreshStateChanged(ScrollRefreshState.DONE);
		}

		mIsEnableDispatch = true;
	}

	/**
	 * 通知下拉刷新
	 */
	private void onPullDownRefresh() {
		if (mIsPullDownRefreshEnable && mPullDownRefreshListener != null) {
			mPullDownRefreshListener.onPullDownRefresh();
		}
	}

	/**
	 * 通知上拉加载
	 */
	private void onPullUpRefresh() {
		if (mIsPullUpRefreshEnable && mPullUpRefreshListener != null) {
			mPullUpRefreshListener.onPullUpRefresh();
		}
	}

	/**
	 * 通知滑动加载
	 */
	private void onScrollRefresh() {
		if (mIsScrollRefreshEnable && mScrollRefreshListener != null) {
			mScrollRefreshListener.onScrollRefresh();
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		onRefreshViewTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}

	@Override
	public void setSelfDispatchEnable(boolean enable) {
		if (mIsEnableDispatch && !enable
				&& (mIsPullDownEventDetected || mIsPullUpEventDetected)) {
			MotionEvent cancelEvent = MotionEvent.obtain(0, 0,
					MotionEvent.ACTION_CANCEL, 0, 0, 0);
			onRefreshViewTouchEvent(cancelEvent);
			cancelEvent.recycle();
		}
		mIsEnableDispatch = enable;
	}

	/**
	 * 配置刷新的触摸事件
	 * 
	 * @param ev
	 */
	private void onRefreshViewTouchEvent(MotionEvent ev) {
		if (!mIsEnableDispatch) {
			return;
		}
		// 下拉刷新事件
		if (mIsPullDownRefreshEnable
				&& (mIsPullDownEventDetected || mContentView.isReachTheTop())) {
			onPullDownTouchEvent(ev);
		}

		// 上拉加载事件
		if (mIsPullUpRefreshEnable
				&& (mIsPullUpEventDetected || mContentView.isReachTheBottom())) {
			onPullUpTouchEvent(ev);
		}

		// 针对于填入IRefreshView时，对Header、Footer的显示矫正
		if (!(mContentView instanceof IRefreshListView)) {
			onScrollTouchEvent(ev);
		}
	}

	/**
	 * 拦截当前滑动事件，取消掉子View的事件拦截行为
	 * 
	 * @param view
	 */
	private void cancelChildDispatch(View view) {
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				View childView = ((ViewGroup) view).getChildAt(i);
				cancelChildDispatch(childView);
			}
		}
		MotionEvent cancelEvent = MotionEvent.obtain(0, 0,
				MotionEvent.ACTION_CANCEL & MotionEvent.ACTION_MASK, 0, 0, 0);
		view.dispatchTouchEvent(cancelEvent);
		cancelEvent.recycle();
	}

	/**
	 * 下拉刷新事件分发
	 * 
	 * @param event
	 */
	private void onPullDownTouchEvent(MotionEvent event) {
		// 记录初始状态
		if (!mIsPullDownEventDetected) {
			mIsPullDownEventDetected = true;
			mPullDownEventStartY = (int) event.getY();
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mPullDownRefreshState != PullDownRefreshState.REFRESHING) {
				if (mPullDownRefreshState == PullDownRefreshState.RELEASE_TO_REFRESH) {
					onPullDownStateChanged(PullDownRefreshState.REFRESHING);
				} else if (mPullDownRefreshState == PullDownRefreshState.NOMORE) {
					onPullDownStateChanged(PullDownRefreshState.NOMORE);
				} else {
					onPullDownStateChanged(PullDownRefreshState.DONE);
				}
			}

			mIsPullDownEventDetected = false;
			mPullDownEventStartY = 0;
			break;
		case MotionEvent.ACTION_MOVE:
			clearAnimation();
			int distanceY = (int) ((event.getY() - mPullDownEventStartY) / mPullDownView
					.getPullRatio());

			// 若为拖拽事件，则取消掉其他子View的事件拦截
			if (distanceY > 10 * Utility.getDensity(getContext())) {
				cancelChildDispatch((View) mContentView);
			}

			if (mPullDownRefreshState != PullDownRefreshState.REFRESHING) {

				// 保证在设置padding的过程中，当前的位置一直是在head，否则如果当列表超出屏幕的话，当在上推的时候，列表会同时进行滚动

				// 可以松手去刷新了
				if (mPullDownRefreshState == PullDownRefreshState.RELEASE_TO_REFRESH) {
					mContentView.scrollToTop(false);

					// 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
					if (distanceY < mPullDownView.getTriggerHeight()
							&& distanceY > mPullDownView.getBaseClipHeight()) {
						onPullDownStateChanged(PullDownRefreshState.PULL_TO_REFRESH);
						Log.i(TAG,
								"Change from RELEASE_To_REFRESH to PULL_To_REFRESH");
					}
					// 一下子推到顶了
					else if (distanceY <= mPullDownView.getBaseClipHeight()) {
						onPullDownStateChanged(PullDownRefreshState.DONE);
						Log.i(TAG, "Change from RELEASE_To_REFRESH to DONE");
					}
					// 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
					else {
						// 不用进行特别的操作，只用更新paddingTop的值就行了
					}
				}
				// 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
				if (mPullDownRefreshState == PullDownRefreshState.PULL_TO_REFRESH) {
					mContentView.scrollToTop(false);

					// 下拉到可以进入RELEASE_TO_REFRESH的状态，并增加额外条件判断canAccessPullToRefresh()
					if (distanceY >= mPullDownView.getTriggerHeight()
							&& isEnableLoadAndRefresh()) {
						onPullDownStateChanged(PullDownRefreshState.RELEASE_TO_REFRESH);
					}
					// 上推到顶了
					else if (distanceY <= mPullDownView.getBaseClipHeight()) {
						onPullDownStateChanged(PullDownRefreshState.DONE);
					}
				}

				// done状态下
				if (mPullDownRefreshState == PullDownRefreshState.DONE) {
					if (distanceY > mPullDownView.getBaseClipHeight()) {
						onPullDownStateChanged(PullDownRefreshState.PULL_TO_REFRESH);
					}
				}

				// nomore状态下
				if (mPullDownRefreshState == PullDownRefreshState.NOMORE
						&& distanceY > mPullDownView.getBaseClipHeight()) {
					mContentView.scrollToTop(false);
				}

				// 更新headView的paddingTop
				if (mPullDownRefreshState == PullDownRefreshState.RELEASE_TO_REFRESH
						|| mPullDownRefreshState == PullDownRefreshState.PULL_TO_REFRESH
						|| mPullDownRefreshState == PullDownRefreshState.NOMORE) {
					mPullDownView.setClipHeight(distanceY);
					mPullDownView.onPullDown(mPullDownRefreshState,
							mPullDownView.getProgress());
				}
			}
			break;
		}
	}

	/**
	 * 上拉加载事件分发
	 * 
	 * @param event
	 */
	private void onPullUpTouchEvent(MotionEvent event) {
		// 记录初始状态
		if (!mIsPullUpEventDetected) {
			mIsPullUpEventDetected = true;
			mPullUpEventStartY = (int) event.getY();
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mPullUpRefreshState != PullUpRefreshState.REFRESHING) {
				if (mPullUpRefreshState == PullUpRefreshState.RELEASE_TO_REFRESH) {
					onPullUpStateChanged(PullUpRefreshState.REFRESHING);
				} else if (mPullUpRefreshState == PullUpRefreshState.NOMORE) {
					onPullUpStateChanged(PullUpRefreshState.NOMORE);
				} else {
					onPullUpStateChanged(PullUpRefreshState.DONE);
				}
			}
			mIsPullUpEventDetected = false;
			mPullUpEventStartY = 0;
			break;
		case MotionEvent.ACTION_MOVE:
			clearAnimation();

			int distanceY = -(int) ((event.getY() - mPullUpEventStartY) / mPullUpView
					.getPullRatio());

			// 若为拖拽事件，则取消掉其他子View的事件拦截
			if (distanceY > 10 * Utility.getDensity(getContext())) {
				cancelChildDispatch((View) mContentView);
			}

			if (mPullUpRefreshState != PullUpRefreshState.REFRESHING) {
				// 可以松手去刷新了
				if (mPullUpRefreshState == PullUpRefreshState.RELEASE_TO_REFRESH) {
					mContentView.scrollToBottom(false);
					// 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
					if (distanceY < mPullUpView.getTriggerHeight()
							&& distanceY > mPullUpView.getBaseClipHeight()) {
						onPullUpStateChanged(PullUpRefreshState.PULL_TO_REFRESH);
					}
					// 一下子推到顶了
					else if (distanceY <= mPullUpView.getBaseClipHeight()) {
						onPullUpStateChanged(PullUpRefreshState.DONE);
					}
					// 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
					else {
						// 不用进行特别的操作，只用更新paddingTop的值就行了
					}
				}
				// 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
				if (mPullUpRefreshState == PullUpRefreshState.PULL_TO_REFRESH) {
					mContentView.scrollToBottom(false);
					// 下拉到可以进入RELEASE_TO_REFRESH的状态，并增加额外条件判断canAccessPullToRefresh()
					if (distanceY >= mPullUpView.getTriggerHeight()
							&& isEnableLoadAndRefresh()) {
						onPullUpStateChanged(PullUpRefreshState.RELEASE_TO_REFRESH);
					}
					// 上推到顶了
					else if (distanceY <= mPullUpView.getBaseClipHeight()) {
						onPullUpStateChanged(PullUpRefreshState.DONE);
					}
				}

				// done状态下
				if (mPullUpRefreshState == PullUpRefreshState.DONE) {
					if (distanceY > mPullUpView.getBaseClipHeight()) {
						onPullUpStateChanged(PullUpRefreshState.PULL_TO_REFRESH);
					}
				}

				// nomore状态下
				if (mPullUpRefreshState == PullUpRefreshState.NOMORE
						&& distanceY > mPullUpView.getBaseClipHeight()) {
					mContentView.scrollToBottom(false);
				}

				// 更新headView的paddingTop
				if (mPullUpRefreshState == PullUpRefreshState.RELEASE_TO_REFRESH
						|| mPullUpRefreshState == PullUpRefreshState.PULL_TO_REFRESH
						|| mPullUpRefreshState == PullUpRefreshState.NOMORE) {
					mPullUpView.setClipHeight(distanceY);
					mPullUpView.onPullUp(mPullUpRefreshState,
							mPullUpView.getProgress());
				}
			}
			break;
		}
	}

	/**
	 * 滚动事件分发
	 * 
	 * @param event
	 */
	private void onScrollTouchEvent(MotionEvent event) {
		if (mScrollTouchEventRecordY == Float.MAX_VALUE) {
			mScrollTouchEventRecordY = event.getY();
		}
		float y = event.getY();
		float dy = y - mScrollTouchEventRecordY;
		mScrollTouchEventRecordY = y;

		if (event.getAction() != MotionEvent.ACTION_MOVE) {
			return;
		}

		// 矫正scrollview的header
		if (mIsPullDownRefreshEnable
				&& mPullDownRefreshState == PullDownRefreshState.REFRESHING) {
			if (mContentView.isReachTheTop()
					|| mPullDownView.getClipHeight() > 0) {
				int clipHeight = (int) Math.max(0, Math.min(
						dy + mPullDownView.getClipHeight(),
						mPullDownView.getTriggerHeight()));
				mPullDownView.setClipHeight(clipHeight);
				mPullDownView.onPullDown(mPullDownRefreshState,
						mPullDownView.getProgress());
				mContentView.scrollToTop(false);
			}
		}

		// 矫正scrollview的footer
		if (mIsPullUpRefreshEnable
				&& mPullUpRefreshState == PullUpRefreshState.REFRESHING) {
			if (mContentView.isReachTheBottom()
					|| mPullUpView.getClipHeight() > 0) {
				int clipHeight = (int) Math.max(
						0,
						Math.min(-dy + mPullUpView.getClipHeight(),
								mPullUpView.getTriggerHeight()));
				mPullUpView.setClipHeight(clipHeight);
				mPullUpView.onPullUp(mPullUpRefreshState,
						mPullUpView.getProgress());
				mContentView.scrollToBottom(false);
			}
		}
	}

	/** 滑动事件监听 */
	private OnScrollToBottomListener mOnScrollListener = new OnScrollToBottomListener() {

		@Override
		public void onScrollToBottom() {
			if (isEnableLoadAndRefresh()
					&& mScrollRefreshState == ScrollRefreshState.DONE) {
				onScrollRefreshStateChanged(ScrollRefreshState.REFRESHING);
			}
		}
	};

	/**
	 * 判断当前是否可以进行加载或刷新
	 * 
	 * @return
	 */
	private boolean isEnableLoadAndRefresh() {
		// 若正在进行上提加载
		if (mPullUpRefreshState == PullUpRefreshState.REFRESHING) {
			return false;
			// 若正在进行滚动加载
		} else if (mScrollRefreshState == ScrollRefreshState.REFRESHING) {
			return false;
		} else if (mPullDownRefreshState == PullDownRefreshState.REFRESHING) {
			return false;
		}
		return true;
	}

	/**
	 * 变更下拉View状态,位移
	 * 
	 * @param newState
	 */
	private void onPullDownStateChanged(PullDownRefreshState newState) {
		if (!mIsPullDownRefreshEnable) {
			return;
		}

		PullDownRefreshState oldState = mPullDownRefreshState;
		mPullDownRefreshState = newState;
		mPullDownView.onStateChanged(oldState, newState);

		switch (newState) {
		case RELEASE_TO_REFRESH:
			Log.i(TAG, "Set PullDown release to refresh");
			break;
		case PULL_TO_REFRESH:
			Log.i(TAG, "Set PullDown pulldown to refresh");
			break;
		case REFRESHING:
			runPullRefreshViewAnim(mPullDownView,
					mPullDownView.getTriggerHeight());
			onPullDownRefresh();
			Log.i(TAG, "Set PullDown refreshing");
			break;
		case DONE:
			runPullRefreshViewAnim(mPullDownView,
					mPullDownView.getBaseClipHeight());
			Log.i(TAG, "Set PullDown done");
			break;
		case NOMORE:
			runPullRefreshViewAnim(mPullDownView,
					mPullDownView.getBaseClipHeight());
			Log.i(TAG, "Set PullDown nomore");
			break;
		}
	}

	/**
	 * 变更上拉View状态,位移
	 * 
	 * @param newState
	 */
	private void onPullUpStateChanged(PullUpRefreshState newState) {
		if (!mIsPullUpRefreshEnable) {
			return;
		}

		PullUpRefreshState oldState = mPullUpRefreshState;
		mPullUpRefreshState = newState;
		mPullUpView.onStateChanged(oldState, newState);

		switch (newState) {
		case RELEASE_TO_REFRESH:
			if (mPullUpView.isRefreshImmediate()) {
				onPullUpStateChanged(PullUpRefreshState.REFRESHING);
			}
			Log.i(TAG, "Set PullUp release to refresh");
			break;
		case PULL_TO_REFRESH:
			Log.i(TAG, "Set PullUp pulldown to refresh");
			break;
		case REFRESHING:
			runPullRefreshViewAnim(mPullUpView, mPullUpView.getTriggerHeight());
			onPullUpRefresh();
			Log.i(TAG, "Set PullUp refreshing");
			break;
		case DONE:
			runPullRefreshViewAnim(mPullUpView, mPullUpView.getBaseClipHeight());
			Log.i(TAG, "Set PullUp done");
			break;
		case NOMORE:
			runPullRefreshViewAnim(mPullUpView, mPullUpView.getBaseClipHeight());
			Log.i(TAG, "Set PullUp nomore");
			break;
		}

	}

	/**
	 * 变更滑动加载View状态
	 * 
	 * @param newState
	 */
	private void onScrollRefreshStateChanged(ScrollRefreshState newState) {
		if (!mIsScrollRefreshEnable) {
			return;
		}

		ScrollRefreshState oldState = mScrollRefreshState;
		mScrollRefreshState = newState;
		mScrollRefreshView.onStateChanged(oldState, newState);

		switch (newState) {
		case REFRESHING:
			mScrollRefreshView.getContentView().setOnClickListener(null);
			onScrollRefresh();
			break;
		case DONE:
			mScrollRefreshView.getContentView().setOnClickListener(null);
			break;
		case FAIL:
			mScrollRefreshView.getContentView().setOnClickListener(
					new OnSingleClickListener() {
						@Override
						public void onSingleClick(View v) {
							onScrollRefreshStateChanged(ScrollRefreshState.REFRESHING);
						}
					});
			break;
		case NOMORE:
			mScrollRefreshView.getContentView().setOnClickListener(null);
			break;
		}
	}

	/**
	 * 偏移到指定位置创建补间动画
	 * 
	 * @param offSet
	 */
	private void runPullRefreshViewAnim(BasePullRefreshView view, int clipHeight) {
		int duration = view.getPullAnimDuration();
		PullRefreshAnimation pullRefreshAnimation = new PullRefreshAnimation(
				view, clipHeight, duration);
		pullRefreshAnimation.startAnimation();
	}

	/**
	 * 实现刷新功能的View所需实现接口
	 * 
	 * @author Calvin
	 * 
	 */
	public interface IRefreshView {

		/**
		 * 是否处于页面的顶部，用于计算衡量展开头部下拉View
		 * 
		 * @return
		 */
		public boolean isReachTheTop();

		/**
		 * 是否处于页面的底部，用于计算衡量展开底部上拉View
		 * 
		 * @return
		 */
		public boolean isReachTheBottom();

		/**
		 * 添加对滑动到底部触发滑动加载的事件监听
		 * 
		 * @param listener
		 */
		public void setOnScrollToBottomListener(
				OnScrollToBottomListener listener);

		/**
		 * 滚动到页面顶部
		 * 
		 * @param isAnim
		 */
		public void scrollToTop(boolean isAnim);

		/**
		 * 滚动到页面底部
		 * 
		 * @param isAnim
		 */
		public void scrollToBottom(boolean isAnim);

	}

	/**
	 * 实现刷新功能的ScrollView所需实现接口
	 * 
	 * @author Calvin
	 * 
	 */
	public interface IRefreshScrollView extends IRefreshView {

	}

	/**
	 * 实现刷新功能的ListView所需实现接口
	 * 
	 * @author Calvin
	 * 
	 */
	public interface IRefreshListView extends IRefreshView {

		/**
		 * 添加顶部下拉View到ListView中
		 * 
		 * @param headerView
		 */
		public void addHeaderRefreshView(View headerView);

		/**
		 * 添加底部上拉或提示加载View到ListView中
		 * 
		 * @param footerView
		 */
		public void addFooterRefreshView(View footerView);

	}

	/**
	 * 滑动到底部触发事件监听
	 * 
	 * @author Calvin
	 * 
	 */
	public interface OnScrollToBottomListener {

		/**
		 * 滑动到View底部
		 */
		public void onScrollToBottom();
	}

	/**
	 * 获取当前列表状态
	 * 
	 * @author Calvin
	 * 
	 */
	public enum IFRefreshViewState {
		WAITING_PULL_DOWN_REFRESH_RESULT, WAITING_PULL_UP_REFRESH_RESULT, WAITING_SCROLLREFRESH_RESULT, NORMAL
	}

	/**
	 * 下拉刷新状态
	 * 
	 * @see #RELEASE_TO_REFRESH 释放后刷新
	 * @see #PULL_TO_REFRESH 下拉后刷新
	 * @see #REFRESHING 正在刷新
	 * @see #DONE 正常状态
	 * @see #NOMORE 无可用加载项
	 * 
	 * @author xuwei
	 * 
	 */
	private static enum PullDownRefreshState {
		RELEASE_TO_REFRESH, PULL_TO_REFRESH, REFRESHING, DONE, NOMORE
	}

	/**
	 * 上拉加载状态
	 * 
	 * @see #RELEASE_TO_REFRESH 释放后加载
	 * @see #PULL_TO_REFRESH 上拉后加载
	 * @see #REFRESHING 正在加载
	 * @see #DONE 正常状态
	 * @see #NOMORE 无后续加载项
	 * 
	 * @author xuwei
	 * 
	 */
	private static enum PullUpRefreshState {
		RELEASE_TO_REFRESH, PULL_TO_REFRESH, REFRESHING, DONE, NOMORE
	}

	/**
	 * 滑动刷新状态
	 * 
	 * @see #REFRESHING 正在加载
	 * @see #FAIL 加载失败
	 * @see #DONE 正常状态
	 * @see #NOMORE 无后续加载项
	 * 
	 * @author xuwei
	 * 
	 */
	private static enum ScrollRefreshState {
		REFRESHING, FAIL, DONE, NOMORE
	}

	/**
	 * 下拉刷新事件监听
	 * 
	 * @author xuwei
	 * 
	 */
	public interface OnPullDownRefreshListener {
		public void onPullDownRefresh();
	}

	/**
	 * 上拉加载事件监听
	 * 
	 * @author xuwei
	 * 
	 */
	public interface OnPullUpRefreshListener {
		public void onPullUpRefresh();
	}

	/**
	 * 滑动加载事件监听
	 * 
	 * @author xuwei
	 * 
	 */
	public interface OnScrollRefreshListener {
		public void onScrollRefresh();
	}

	/**
	 * 拖拉刷新View控制基类，{@link BasePullDownRefreshView}继承自此类，重写了
	 * {@link #setClipHeight(int)}方法，实现了下拉刷新的View控制;
	 * 
	 * @author Calvin
	 * 
	 */
	public static abstract class BasePullRefreshView {

		/** 实际手指滑动的距离与界面显示距离的偏移比，例如：手指画过300px距离，则只展示出100px的拉伸，橡皮效果。 */
		private final static int DEFAULT_PULL_RATIO = 3;
		/** 默认的补间动画时长 300ms */
		private final static int DEFAULT_PULL_ANIMATION_DURATION = 300;

		/** context */
		protected Context mContext;
		/** 拖拉View */
		protected View mRefreshContentView;
		/** 拖拉View的高度 */
		protected int mRefreshContentViewHeight;

		/**
		 * 构造
		 * 
		 * @param context
		 */
		public BasePullRefreshView(Context context) {
			mContext = context;
		}

		/**
		 * 供{@link IFRefreshViewLayout}内部获取拉动的View
		 * 
		 * @return
		 */
		protected final View getContentView() {
			if (mRefreshContentView == null) {
				mRefreshContentView = getPullRefreshView();
			}
			return mRefreshContentView;
		}

		/**
		 * 供{@link IFRefreshViewLayout}内部获取拉动的View高度
		 * 
		 * @return
		 */
		protected final int getContentHeight() {
			return mRefreshContentViewHeight;
		}

		/**
		 * 供{@link IFRefreshViewLayout}
		 * 内部获取触发刷新的高度，可用于分割下拉View和之上的水印，实现下拉之后的水印效果。
		 * 
		 * @return
		 */
		protected int getTriggerHeight() {
			return mRefreshContentViewHeight;
		}

		/**
		 * 供{@link IFRefreshViewLayout} 内部获取下拉View露出隐藏区域的高度，可供实现类似path下拉放大背景的效果。
		 * 
		 * @return
		 */
		protected int getBaseClipHeight() {
			return 0;
		}

		/**
		 * 获取当前拖拽进度
		 * 
		 * @return
		 */
		protected final float getProgress() {
			return (float) (getClipHeight() - getBaseClipHeight())
					/ (getTriggerHeight() - getBaseClipHeight());
		}

		/**
		 * 获取动画执行时间
		 * 
		 * @return
		 */
		protected int getPullAnimDuration() {
			return DEFAULT_PULL_ANIMATION_DURATION;
		}

		/**
		 * 获取拉动距离的偏移比，用于实现橡皮筋效果
		 * 
		 * @return
		 */
		protected float getPullRatio() {
			return DEFAULT_PULL_RATIO;
		}

		/**
		 * 设置实现露出隐藏区域的可视高度，注意对{@link #getBaseClipHeight()}的处理。
		 * 
		 * @param clipHeight
		 */
		protected abstract void setClipHeight(int clipHeight);

		/**
		 * 获取当前露出隐藏区域的可视高度
		 * 
		 * @return
		 */
		protected abstract int getClipHeight();

		/**
		 * 生成拉动刷新的View
		 * 
		 * @return
		 */
		protected abstract View getPullRefreshView();
	}

	/**
	 * 下拉刷新View控制基类，可继承自此类重写{@link #getPullRefreshView()}以及
	 * {@link #onStateChanged(PullDownRefreshState, PullDownRefreshState)} 、
	 * {@link #onPullDown(PullDownRefreshState, float)}方法实现自定义View样式
	 * 
	 * @author Calvin
	 * 
	 */
	public static class BasePullDownRefreshView extends BasePullRefreshView {

		/** 上次下拉刷新时间记录 */
		private long mLastUpdateTime;

		/*
		 * 缺省的下拉刷新View配置
		 */

		/** 下拉刷新的状态提示 */
		private TextView mStateTips;
		/** 下拉刷新的更新时间 */
		private TextView mLastUpdatedTime;
		/** 下拉刷新的指示箭头 */
		private ImageView mHintArrow;
		/** 下拉刷新的等待图标 */
		private ProgressBar mProgressBar;

		/** 箭头向下翻转动画 */
		private RotateAnimation mArrowAnimation;
		/** 箭头向上翻转动画 */
		private RotateAnimation mArrowReverseAnimation;

		/**
		 * 构造
		 * 
		 * @param context
		 */
		public BasePullDownRefreshView(Context context) {
			super(context);
			mLastUpdateTime = System.currentTimeMillis();
		}

		@Override
		protected final void setClipHeight(int clipHeight) {
			int paddingTop = clipHeight - getContentHeight();
			mRefreshContentView.setPadding(0, paddingTop, 0, 0);
		}

		@Override
		protected int getClipHeight() {
			return mRefreshContentView.getPaddingTop() + getContentHeight();
		}

		@Override
		protected View getPullRefreshView() {
			LayoutInflater inflater = LayoutInflater.from(mContext);

			View pullRefreshView = (LinearLayout) inflater.inflate(
					R.layout.common_refresh_pull_head, null);

			mHintArrow = (ImageView) pullRefreshView
					.findViewById(R.id.head_arrow);
			mProgressBar = (ProgressBar) pullRefreshView
					.findViewById(R.id.head_progressBar);
			mStateTips = (TextView) pullRefreshView
					.findViewById(R.id.head_tips);
			mLastUpdatedTime = (TextView) pullRefreshView
					.findViewById(R.id.head_lastupdate);

			Utility.measureView(pullRefreshView);

			mRefreshContentViewHeight = pullRefreshView.getMeasuredHeight();

			pullRefreshView.setPadding(0,
					-(getContentHeight() - getBaseClipHeight()), 0, 0);
			pullRefreshView.invalidate();

			mArrowAnimation = new RotateAnimation(0, -180,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f);
			mArrowAnimation.setInterpolator(new LinearInterpolator());
			mArrowAnimation.setDuration(250);
			mArrowAnimation.setFillAfter(true);

			mArrowReverseAnimation = new RotateAnimation(-180, 0,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f);
			mArrowReverseAnimation.setInterpolator(new LinearInterpolator());
			mArrowReverseAnimation.setDuration(200);
			mArrowReverseAnimation.setFillAfter(true);

			return pullRefreshView;
		}

		/**
		 * 下拉刷新View状态发生改变时，需要执行的操作
		 * 
		 * @param pullDownRefreshState
		 */
		protected void onStateChanged(PullDownRefreshState oldState,
				PullDownRefreshState newState) {
			switch (newState) {
			case RELEASE_TO_REFRESH:
				mHintArrow.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.GONE);
				mStateTips.setVisibility(View.VISIBLE);
				mLastUpdatedTime.setVisibility(View.VISIBLE);

				mHintArrow.clearAnimation();
				mHintArrow.startAnimation(mArrowAnimation);

				mStateTips.setText(R.string.common_refresh_release_to_refresh);
				break;
			case PULL_TO_REFRESH:
				mProgressBar.setVisibility(View.GONE);
				mStateTips.setVisibility(View.VISIBLE);
				mLastUpdatedTime.setVisibility(View.VISIBLE);
				mHintArrow.clearAnimation();
				mHintArrow.setVisibility(View.VISIBLE);

				if (oldState == PullDownRefreshState.RELEASE_TO_REFRESH) {
					mHintArrow.clearAnimation();
					mHintArrow.startAnimation(mArrowReverseAnimation);
				} else if (oldState == PullDownRefreshState.DONE) {
					mLastUpdatedTime
							.setText(mContext
									.getString(R.string.common_refresh_last_update)
									+ Utility
											.countLastRefreshHintText(mLastUpdateTime));
				}

				mStateTips
						.setText(R.string.common_refresh_pull_down_to_refresh);
				break;
			case REFRESHING:
				mProgressBar.setVisibility(View.VISIBLE);
				mHintArrow.clearAnimation();
				mHintArrow.setVisibility(View.GONE);
				mStateTips.setText(R.string.common_refresh_refreshing);
				mLastUpdatedTime.setVisibility(View.VISIBLE);
				break;
			case DONE:
				mProgressBar.setVisibility(View.GONE);
				mHintArrow.clearAnimation();
				mHintArrow.setImageResource(R.drawable.image_head_pullrefresh);
				mHintArrow.setVisibility(View.VISIBLE);
				mStateTips
						.setText(R.string.common_refresh_pull_down_to_refresh);
				mLastUpdatedTime.setVisibility(View.VISIBLE);
				mLastUpdatedTime.setText(mContext
						.getString(R.string.common_refresh_last_update)
						+ Utility.countLastRefreshHintText(mLastUpdateTime));
				break;
			case NOMORE:
				mProgressBar.setVisibility(View.GONE);
				mHintArrow.setVisibility(View.GONE);
				mLastUpdatedTime.setVisibility(View.GONE);
				mStateTips.setText(R.string.common_refresh_nomore);
				break;
			}
		}

		/**
		 * 通知下拉刷新的进度变更以及当前所处状态，其中progress参数为{@link #setClipHeight(int)}方法中返还的结果。
		 * 
		 * @param pullDownRefreshState
		 * @param progress
		 */
		protected void onPullDown(PullDownRefreshState pullDownRefreshState,
				float progress) {
			// can be override
		}

		/**
		 * 获取上次成功刷新的时间
		 * 
		 * @return
		 */
		protected final long getLastUpdateTime() {
			return mLastUpdateTime;
		}

		/**
		 * 变更上次刷新时间
		 * 
		 * @param time
		 */
		private final void notifyUpdateTime(long time) {
			mLastUpdateTime = time;
		}

	}

	/**
	 * 上拉加载View控制基类，可继承自此类重写{@link #getPullRefreshView()}以及
	 * {@link #onStateChanged(PullUpRefreshState, PullUpRefreshState)} 、
	 * {@link #onPullUp(PullUpRefreshState,float)}方法实现自定义View样式
	 * 
	 * @author Calvin
	 * 
	 */
	public static class BasePullUpRefreshView extends BasePullRefreshView {

		/** 上次下拉刷新时间记录 */
		private long mLastUpdateTime;
		/** 是否在上拉之后即触发加载 */
		private boolean mRefreshImmediate = false;

		/*
		 * 缺省的上拉加载View配置
		 */

		/** 上拉加载的状态提示 */
		private TextView mStateTips;
		/** 上拉加载的指示箭头 */
		private ImageView mHintArrow;
		/** 上拉加载的等待图标 */
		private ProgressBar mProgressBar;

		/** 箭头向下翻转动画 */
		private RotateAnimation mArrowAnimation;
		/** 箭头向上翻转动画 */
		private RotateAnimation mArrowReverseAnimation;

		/**
		 * 构造
		 * 
		 * @param context
		 */
		public BasePullUpRefreshView(Context context) {
			super(context);
			mLastUpdateTime = System.currentTimeMillis();
		}

		@Override
		protected final void setClipHeight(int clipHeight) {
			int paddingBottom = clipHeight - getContentHeight();
			mRefreshContentView.setPadding(0, 0, 0, paddingBottom);
		}

		@Override
		protected int getClipHeight() {
			return mRefreshContentView.getPaddingBottom() + getContentHeight();
		}

		@Override
		protected View getPullRefreshView() {
			LayoutInflater inflater = LayoutInflater.from(mContext);

			View pullRefreshView = (LinearLayout) inflater.inflate(
					R.layout.common_refresh_pull_foot, null);

			mHintArrow = (ImageView) pullRefreshView
					.findViewById(R.id.foot_arrow);
			mProgressBar = (ProgressBar) pullRefreshView
					.findViewById(R.id.foot_progressBar);
			mStateTips = (TextView) pullRefreshView
					.findViewById(R.id.foot_tips);

			Utility.measureView(pullRefreshView);

			mRefreshContentViewHeight = pullRefreshView.getMeasuredHeight();

			pullRefreshView.setPadding(0, 0, 0,
					-(getContentHeight() - getBaseClipHeight()));
			pullRefreshView.invalidate();

			mArrowAnimation = new RotateAnimation(0, -180,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f);
			mArrowAnimation.setInterpolator(new LinearInterpolator());
			mArrowAnimation.setDuration(250);
			mArrowAnimation.setFillAfter(true);

			mArrowReverseAnimation = new RotateAnimation(-180, 0,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f,
					RotateAnimation.RELATIVE_TO_SELF, 0.5f);
			mArrowReverseAnimation.setInterpolator(new LinearInterpolator());
			mArrowReverseAnimation.setDuration(200);
			mArrowReverseAnimation.setFillAfter(true);

			return pullRefreshView;
		}

		/**
		 * 上拉加载View状态发生改变时，需要执行的操作
		 * 
		 * @param pullUpRefreshState
		 */
		protected void onStateChanged(PullUpRefreshState oldState,
				PullUpRefreshState newState) {
			switch (newState) {
			case RELEASE_TO_REFRESH:
				mHintArrow.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.GONE);
				mStateTips.setVisibility(View.VISIBLE);

				mHintArrow.clearAnimation();
				mHintArrow.startAnimation(mArrowAnimation);

				mStateTips.setText(R.string.common_refresh_release_to_load);
				break;
			case PULL_TO_REFRESH:
				mProgressBar.setVisibility(View.GONE);
				mStateTips.setVisibility(View.VISIBLE);
				mHintArrow.clearAnimation();
				mHintArrow.setVisibility(View.VISIBLE);

				if (oldState == PullUpRefreshState.RELEASE_TO_REFRESH) {
					mHintArrow.clearAnimation();
					mHintArrow.startAnimation(mArrowReverseAnimation);
				}

				mStateTips.setText(R.string.common_refresh_pull_up_to_refresh);
				break;
			case REFRESHING:
				mProgressBar.setVisibility(View.VISIBLE);
				mHintArrow.clearAnimation();
				mHintArrow.setVisibility(View.GONE);
				mStateTips.setText(R.string.common_refresh_loading);
				break;
			case DONE:
				mProgressBar.setVisibility(View.GONE);
				mHintArrow.clearAnimation();
				mHintArrow.setImageResource(R.drawable.image_foot_pullrefresh);
				mHintArrow.setVisibility(View.VISIBLE);
				mStateTips.setText(R.string.common_refresh_pull_up_to_refresh);
				break;
			case NOMORE:
				mProgressBar.setVisibility(View.GONE);
				mHintArrow.setVisibility(View.GONE);
				mStateTips.setText(R.string.common_refresh_nomore);
				break;
			}
		}

		/**
		 * 通知上拉加载的进度变更以及当前所处状态，其中progress参数为{@link #setClipHeight(int)}方法中返还的结果。
		 * 
		 * @param pullDownRefreshState
		 * @param progress
		 */
		protected void onPullUp(PullUpRefreshState pullUpRefreshState,
				float progress) {
			// can be override
		}

		/**
		 * 获取上次成功加载的时间
		 * 
		 * @return
		 */
		protected final long getLastUpdateTime() {
			return mLastUpdateTime;
		}

		/**
		 * 变更上次加载时间
		 * 
		 * @param time
		 */
		private final void notifyUpdateTime(long time) {
			mLastUpdateTime = time;
		}

		/**
		 * 设置是否在上拉后立即加载
		 * 
		 * @param enable
		 */
		protected final void setRefreshImmediate(boolean enable) {
			mRefreshImmediate = enable;
		}

		/**
		 * 获取是否在上拉后立即加载
		 */
		protected final boolean isRefreshImmediate() {
			return mRefreshImmediate;
		}
	}

	/**
	 * 滚动加载View控制基类，可继承自此类重写{@link #getScrollRefreshView()}以及
	 * {@link #onStateChanged(ScrollRefreshState, ScrollRefreshState)}
	 * 方法实现自定义View样式
	 * 
	 * @author Calvin
	 * 
	 */
	public static class BaseScrollRefreshView {

		/** context */
		protected Context mContext;
		/** 滑动加载View */
		protected View mRefreshContentView;
		/** 滑动加载View的高度 */
		protected int mRefreshContentViewHeight;
		/** 上次加载时间记录 */
		private long mLastUpdateTime;

		/*
		 * 缺省的滑动加载View配置
		 */
		/** 滑动加载的FootView的提示文字 */
		private TextView mScrollRefreshTips;
		/** 滑动加载的FootView的提示图标 */
		private ImageView mScrollRefreshWarning;
		/** 滑动加载的FootView的等待图标 */
		private ProgressBar mScrollRefreshProgressBar;

		/**
		 * 构造
		 * 
		 * @param context
		 */
		public BaseScrollRefreshView(Context context) {
			mContext = context;
		}

		/**
		 * 供{@link IFRefreshViewLayout}内部获取的View
		 * 
		 * @return
		 */
		protected final View getContentView() {
			if (mRefreshContentView == null) {
				mRefreshContentView = getScrollRefreshView();
			}
			return mRefreshContentView;
		}

		/**
		 * 供{@link IFRefreshViewLayout}内部获取滚动加载的View高度
		 * 
		 * @return
		 */
		protected final int getContentHeight() {
			return mRefreshContentViewHeight;
		}

		/**
		 * 生成滚动加载的View
		 * 
		 * @return
		 */
		protected View getScrollRefreshView() {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			View scrollRefreshView = (LinearLayout) inflater.inflate(
					R.layout.common_refresh_scroll_foot, null);
			mScrollRefreshWarning = (ImageView) scrollRefreshView
					.findViewById(R.id.foot_warning);
			mScrollRefreshProgressBar = (ProgressBar) scrollRefreshView
					.findViewById(R.id.foot_progressBar);
			mScrollRefreshTips = (TextView) scrollRefreshView
					.findViewById(R.id.foot_tips);

			Utility.measureView(scrollRefreshView);
			mRefreshContentViewHeight = scrollRefreshView.getMeasuredHeight();

			return scrollRefreshView;
		}

		/**
		 * 滚动加载View状态发生改变时，需要执行的操作
		 * 
		 * @param pullUpRefreshState
		 */
		protected void onStateChanged(ScrollRefreshState oldState,
				ScrollRefreshState newState) {
			switch (newState) {
			case REFRESHING:
				mScrollRefreshProgressBar.setVisibility(View.VISIBLE);
				mScrollRefreshWarning.setVisibility(View.GONE);
				mScrollRefreshTips.setText(R.string.common_refresh_loading);
				break;
			case DONE:
				mScrollRefreshProgressBar.setVisibility(View.VISIBLE);
				mScrollRefreshWarning.setVisibility(View.GONE);
				mScrollRefreshTips.setText(R.string.common_refresh_loading);
				mRefreshContentView.setPadding(0, 0, 0, 0);
				break;
			case FAIL:
				mScrollRefreshProgressBar.setVisibility(View.GONE);
				mScrollRefreshWarning.setVisibility(View.VISIBLE);
				mScrollRefreshTips.setText(R.string.common_refresh_load_failed);
				break;
			case NOMORE:
				mRefreshContentView.setPadding(0, -getContentHeight(), 0, 0);
				break;
			}
		}

		/**
		 * 获取上次成功加载的时间
		 * 
		 * @return
		 */
		protected final long getLastUpdateTime() {
			return mLastUpdateTime;
		}

		/**
		 * 变更上次加载时间
		 * 
		 * @param time
		 */
		private final void notifyUpdateTime(long time) {
			mLastUpdateTime = time;
		}
	}

	/**
	 * 拉动View动作补间动画
	 * 
	 * @author Calvin
	 * 
	 */
	private class PullRefreshAnimation extends IFAnimation {

		/** 动画执行View */
		private BasePullRefreshView mTargetView;
		/** 起始点 */
		private int mStart;
		/** 距离差 */
		private int mOffSet;

		public PullRefreshAnimation(BasePullRefreshView view, int clipHeight,
				int duration) {
			super(view.mRefreshContentView, duration);
			mTargetView = view;

			mStart = mTargetView.getClipHeight();
			mOffSet = clipHeight - mStart;
			setInterpolator(new DecelerateInterpolator());
		}

		@Override
		public void applyTransformation(float percent) {
			mTargetView.setClipHeight((int) (mStart + mOffSet * percent));
			if (mTargetView instanceof BasePullDownRefreshView) {
				((BasePullDownRefreshView) mTargetView).onPullDown(
						mPullDownRefreshState, mTargetView.getProgress());
				mContentView.scrollToTop(false);
			} else if (mTargetView instanceof BasePullUpRefreshView) {
				((BasePullUpRefreshView) mTargetView).onPullUp(
						mPullUpRefreshState, mTargetView.getProgress());
				mContentView.scrollToBottom(false);
			}
		}

		@Override
		public void onAnimationFinished() {

		}
	}

}
