package com.ifeng.util.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.ifeng.util.ui.IFRefreshViewLayout.IRefreshListView;
import com.ifeng.util.ui.IFRefreshViewLayout.IRefreshView;
import com.ifeng.util.ui.IFRefreshViewLayout.OnScrollToBottomListener;

/**
 * 作为{@link IFRefreshListView}
 * 的增强版实现，提供对于悬浮SectionView的点击功能支撑，针对对Section操作有更多需求的功能进行满足。
 * IFRefreshViewLayout.setContentView(new IFRefreshSectionListView(Context,
 * IFRefreshListView));即可完成设置
 * 
 * @author Calvin
 * 
 */
public class IFRefreshSectionListView extends ViewGroup implements
		IRefreshListView {

	/** 当前是否已经添加了悬浮View */
	private boolean mIsPinnedViewExist;

	/**
	 * 传入ListView方式构造
	 * 
	 * @param context
	 * @param listView
	 */
	public IFRefreshSectionListView(Context context, IFRefreshListView listView) {
		super(context);
		listView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		addView(listView);
	}

	/**
	 * xml layout方式构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public IFRefreshSectionListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		for (int i = 0; i < getChildCount(); i++) {
			View view = getChildAt(i);
			if (view instanceof IFRefreshListView) {
				view.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
			} else {
				view.layout(view.getLeft(), view.getTop(), view.getRight(),
						view.getBottom());
			}
		}
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
		int measureHeigth = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(measureWidth, measureHeigth);

		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);

			int widthSpec = 0;
			int heightSpec = 0;

			LayoutParams params = v.getLayoutParams();

			if (params.width > 0) {
				widthSpec = MeasureSpec.makeMeasureSpec(params.width,
						MeasureSpec.EXACTLY);
			} else if (params.width == LayoutParams.FILL_PARENT) {
				widthSpec = MeasureSpec.makeMeasureSpec(measureWidth,
						MeasureSpec.EXACTLY);
			} else if (params.width == LayoutParams.WRAP_CONTENT) {
				widthSpec = MeasureSpec.makeMeasureSpec(measureWidth,
						MeasureSpec.AT_MOST);
			}

			if (params.height > 0) {
				heightSpec = MeasureSpec.makeMeasureSpec(params.height,
						MeasureSpec.EXACTLY);
			} else if (params.height == LayoutParams.FILL_PARENT) {
				heightSpec = MeasureSpec.makeMeasureSpec(measureHeigth,
						MeasureSpec.EXACTLY);
			} else if (params.height == LayoutParams.WRAP_CONTENT) {
				heightSpec = MeasureSpec.makeMeasureSpec(measureWidth,
						MeasureSpec.AT_MOST);
			}

			v.measure(widthSpec, heightSpec);
		}
	}

	/**
	 * 显示悬浮view
	 * 
	 * @param sectionView
	 */
	protected void displaySectionView(View sectionView) {
		if (!mIsPinnedViewExist) {
			addView(sectionView);
			mIsPinnedViewExist = true;
		} else {
			sectionView.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * 隐藏悬浮view
	 * 
	 * @param sectionView
	 */
	protected void dismissSectionView(View sectionView) {
		if (mIsPinnedViewExist) {
			sectionView.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean isReachTheTop() {
		IRefreshView listView = (IRefreshView) getChildAt(0);
		return listView.isReachTheTop();
	}

	@Override
	public boolean isReachTheBottom() {
		IRefreshView listView = (IRefreshView) getChildAt(0);
		return listView.isReachTheBottom();
	}

	@Override
	public void setOnScrollToBottomListener(OnScrollToBottomListener listener) {
		IRefreshView listView = (IRefreshView) getChildAt(0);
		listView.setOnScrollToBottomListener(listener);
	}

	@Override
	public void scrollToTop(boolean isAnim) {
		IRefreshView listView = (IRefreshView) getChildAt(0);
		listView.scrollToTop(isAnim);
	}

	@Override
	public void scrollToBottom(boolean isAnim) {
		IRefreshView listView = (IRefreshView) getChildAt(0);
		listView.scrollToBottom(isAnim);
	}

	@Override
	public void addHeaderRefreshView(View headerView) {
		IRefreshListView listView = (IRefreshListView) getChildAt(0);
		listView.addHeaderRefreshView(headerView);
	}

	@Override
	public void addFooterRefreshView(View footerView) {
		IRefreshListView listView = (IRefreshListView) getChildAt(0);
		listView.addFooterRefreshView(footerView);
	}

}
