package com.ifeng.util.ui;

import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.ifeng.BaseApplicaion;
import com.ifeng.util.Utility;
import com.ifeng.util.imagecache.Utils;
import com.ifeng.util.logging.Log;
import com.ifeng.util.ui.IFRefreshViewLayout.IRefreshListView;
import com.ifeng.util.ui.IFRefreshViewLayout.OnScrollToBottomListener;
import com.ifeng.util.ui.adapter.list.BaseSectionAdapter;

/**
 * 集成的PinnedHeader的ListView，并实现了{@link IFRefreshListView}
 * 的接口，若需实现下拉刷新、上拉加载、滚动加载等功能则在
 * {@link IFRefreshViewLayout#setContentView(IFRefreshViewLayout.IRefreshView)}
 * 方法中传入本类实例即可 ，PinnedHeader功能需要在 {@link #setAdapter(BaseAdapter)}时传入
 * {@link BaseSectionAdapter}即可生效
 * 
 * @author xuwei
 * 
 */
public class IFRefreshListView extends ListView implements OnScrollListener,
		IRefreshListView {

	/** Tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug开关 */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;

	/*
	 * PinnedHeader配置
	 */
	/** 是否开启悬浮section */
	private boolean mIsPinnedSectionOn;
	/** section adapter */
	private BaseSectionAdapter mSectionAdapter;
	/** 悬浮在顶部的SectionView */
	private View mPinnedSectionView;
	/** 是否显示PinedView */
	private boolean mIsPinnedSectionVisible;
	/** section展现状态监听 */
	private PinnedSectionListener mSectionListener;
	/** 当前所选section */
	private int mCurrentSection;

	/*
	 * IRefreshListView配置
	 */
	/** 滑动到底部的监听 */
	private OnScrollToBottomListener mScrollToBottomListener;
	/** addheaderview方法执行记录，用于将下拉View置顶 */
	private LinkedList<View> mHeadViewRecord;
	/** addfooterview方法执行记录，用于将上拉View置末 */
	private LinkedList<View> mFootViewRecord;

	/**
	 * 构造
	 * 
	 * @param context
	 */
	public IFRefreshListView(Context context) {
		super(context);
		init();
	}

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public IFRefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * 初始化下拉刷新和滑动加载的View
	 */
	@TargetApi(9)
	private void init() {
		setCacheColorHint(Color.TRANSPARENT);
		setDivider(null);
		setVerticalFadingEdgeEnabled(false);
		setOnScrollListener(this);
		if (Utils.hasGingerbread()) {
			setOverScrollMode(View.OVER_SCROLL_NEVER);
		}
		mHeadViewRecord = new LinkedList<View>();
		mFootViewRecord = new LinkedList<View>();
	}

	/**
	 * 监听滑动事件，记录当前第一条可见列表项的索引值，捕捉页面滚动至最下方的事件
	 */
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

		if (mScrollToBottomListener != null
				&& firstVisibleItem + visibleItemCount == totalItemCount) {
			mScrollToBottomListener.onScrollToBottom();
		}

		if (mSectionListener != null && mSectionAdapter != null) {
			int section = mSectionAdapter.getSectionId(Math.min(
					mSectionAdapter.getCount(),
					Math.max(0, firstVisibleItem - getHeaderViewsCount())));
			if (section != mCurrentSection) {
				mSectionListener.onSectionChanged(section);
				mCurrentSection = section;
			}
		}

		configurePinnedSectionView(firstVisibleItem - getHeaderViewsCount());
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mExtraOnScrollListener != null) {
			mExtraOnScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	/** 设定额外的OnScrollListener */
	private OnScrollListener mExtraOnScrollListener;

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		super.setOnScrollListener(this);
		if (l != this) {
			mExtraOnScrollListener = l;
		}
	}

	/**
	 * 设置{@link BaseSectionAdapter},默认开启悬停SectionView
	 * 
	 * @param sectionAdapter
	 */
	public void setAdapter(BaseSectionAdapter sectionAdapter) {
		setAdapter(sectionAdapter, true);
	}

	/**
	 * 设置{@link BaseSectionAdapter}
	 * 
	 * @param sectionAdapter
	 * @param needPinned
	 *            是否需要悬停的SectionView
	 */
	public void setAdapter(BaseSectionAdapter sectionAdapter, boolean needPinned) {
		// 配置PinnedAdapter
		if (sectionAdapter != null
				&& sectionAdapter instanceof BaseSectionAdapter) {
			mSectionAdapter = sectionAdapter;
			mIsPinnedSectionOn = needPinned;
		}
		super.setAdapter(sectionAdapter);
	}

	@Override
	protected void layoutChildren() {
		try {
			super.layoutChildren();
		} catch (IllegalStateException e) {
			Log.e(TAG, "This is not realy dangerous problem");
		}

		if (mIsPinnedSectionOn && mPinnedSectionView == null) {
			mPinnedSectionView = mSectionAdapter.getSectionMouldView();
			mPinnedSectionView.setLayoutParams(new LayoutParams(
					getMeasuredWidth(),
					RelativeLayout.LayoutParams.WRAP_CONTENT));

			mPinnedSectionView.measure(MeasureSpec.makeMeasureSpec(
					getMeasuredWidth(), MeasureSpec.EXACTLY), MeasureSpec
					.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			mPinnedSectionView.layout(0, 0, getMeasuredWidth(),
					mPinnedSectionView.getMeasuredHeight());
		}
	}

	/*
	 * PinnedHeader部分
	 */

	/**
	 * 配置对应位置的headerview
	 * 
	 * @param position
	 *            listview
	 */
	public void configurePinnedSectionView(int position) {
		if (mPinnedSectionView == null) {
			return;
		}

		if (!mIsPinnedSectionOn) {
			return;
		}

		PinnedSectionState state = getPinnedSectionState(position);
		switch (state) {
		case PINNED_HEADER_GONE:
			mIsPinnedSectionVisible = false;
			break;

		case PINNED_HEADER_VISIBLE:
			mPinnedSectionView.layout(0, 0, getMeasuredWidth(),
					mPinnedSectionView.getMeasuredHeight());
			mIsPinnedSectionVisible = true;
			break;

		case PINNED_HEADER_CLIP:
			View firstView = getChildAt(0);
			int bottom = firstView.getBottom();
			int headerHeight = mPinnedSectionView.getHeight();
			int y;
			if (bottom < headerHeight && headerHeight != 0) {
				y = (bottom - headerHeight);
			} else {
				y = 0;
			}
			if (mPinnedSectionView.getTop() != y) {
				mPinnedSectionView.layout(0, y, getMeasuredWidth(),
						mPinnedSectionView.getMeasuredHeight() + y);
			}
			mIsPinnedSectionVisible = true;
			break;
		}
	}

	/**
	 * 跳转至指定section
	 * 
	 * @param position
	 */
	@SuppressLint("NewApi")
	public void setSelectSection(int position) {
		if (mSectionAdapter == null) {
			return;
		}

		int realPosition = 0;
		for (int i = 0; i < position; i++) {
			realPosition++;
			realPosition += mSectionAdapter.getCount(i);
		}

		if (Utils.hasFroyo()) {
			smoothScrollToPosition(realPosition + getHeaderViewsCount());
		} else {
			setSelection(realPosition + getHeaderViewsCount());
		}

	}

	@SuppressLint("NewApi")
	@Override
	public void smoothScrollToPosition(int position) {
		if (getFirstVisiblePosition() <= position
				&& getLastVisiblePosition() >= position) {
			if (Utils.hasHoneycomb()) {
				smoothScrollToPositionFromTop(position,
						Utility.getStatusBarHeight(getContext()));
			} else {
				setSelection(position);
			}
		} else {
			super.smoothScrollToPosition(position);
		}
	}

	/**
	 * 添加section展示状态监听
	 * 
	 * @param mSectionListener
	 */
	public void setSectionListener(PinnedSectionListener sectionListener) {
		mSectionListener = sectionListener;
	}

	/**
	 * 获取当前悬浮Section显示状态
	 * 
	 * @param position
	 * @return
	 */
	private PinnedSectionState getPinnedSectionState(int position) {
		if (!mIsPinnedSectionOn) {
			return PinnedSectionState.PINNED_HEADER_GONE;
		}

		if (mSectionAdapter.getPositionIsSection(position + 1)) {
			if (position <= 0) {
				return PinnedSectionState.PINNED_HEADER_GONE;
			}
			return PinnedSectionState.PINNED_HEADER_CLIP;
		} else {
			return PinnedSectionState.PINNED_HEADER_VISIBLE;
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mIsPinnedSectionVisible) {
			mPinnedSectionView = mSectionAdapter.getSectionView(
					mPinnedSectionView,
					mSectionAdapter.getSectionId(getFirstVisiblePosition()
							- getHeaderViewsCount()));

			if (getParent() instanceof IFRefreshSectionListView) {
				((IFRefreshSectionListView) getParent())
						.displaySectionView(mPinnedSectionView);
			}

			drawChild(canvas, mPinnedSectionView, getDrawingTime());
		} else if (getParent() instanceof IFRefreshSectionListView) {
			((IFRefreshSectionListView) getParent())
					.dismissSectionView(mPinnedSectionView);
		}
	}

	/**
	 * PinnedSection状态
	 * 
	 * @author Calvin
	 * 
	 */
	public enum PinnedSectionState {
		/** 不显示Section，或为下拉刷新状态时 */
		PINNED_HEADER_GONE,
		/** 悬浮在顶部显示的Section */
		PINNED_HEADER_VISIBLE,
		/** 被下一个Section推挤，或者下拉即将显示上一个Section */
		PINNED_HEADER_CLIP
	}

	/**
	 * section展现状态监听
	 * 
	 * @author Calvin
	 * 
	 */
	public interface PinnedSectionListener {

		/**
		 * 通知当前section发生变化
		 * 
		 * @param section
		 */
		public void onSectionChanged(int section);
	}

	/*
	 * IFRefreshViewLayout部分接口配置
	 * 
	 * @see com.ifeng.util.ui.IFRefreshViewLayout.IRefreshView#isReachTheTop()
	 */

	@Override
	public boolean isReachTheTop() {
		return getFirstVisiblePosition() == 0;
	}

	@Override
	public boolean isReachTheBottom() {
		try {
			if (getLastVisiblePosition() == getAdapter().getCount() - 1) {
				return true;
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return false;
	}

	@Override
	public void setOnScrollToBottomListener(OnScrollToBottomListener listener) {
		mScrollToBottomListener = listener;
	}

	@Override
	public void addHeaderRefreshView(View headerView) {
		super.addHeaderView(headerView, null, false);
	}

	@Override
	public void addFooterRefreshView(View footerView) {
		super.addFooterView(footerView, null, false);
	}

	@Override
	public void scrollToTop(boolean isAnim) {
		if (isAnim && Utils.hasFroyo()) {
			smoothScrollToPosition(0);
		} else {
			setSelection(0);
		}
	}

	@Override
	public void scrollToBottom(boolean isAnim) {
		if (isAnim && Utils.hasFroyo()) {
			smoothScrollToPosition(getAdapter().getCount() - 1);
		} else {
			setSelection(getAdapter().getCount() - 1);
		}
	}

}
