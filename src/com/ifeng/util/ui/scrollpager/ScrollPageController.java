package com.ifeng.util.ui.scrollpager;

import java.util.LinkedList;
import java.util.List;

import android.view.View;

import com.ifeng.util.ui.scrollpager.ScrollPageView.ScrollPageListener;

/**
 * 滑动翻页的控制类
 * 
 * @author xuwei
 * 
 */
public abstract class ScrollPageController implements ScrollPageListener {

	/** 缺省的预加载分页数，默认为1，则为 左 中 右 三个分页。 */
	public static final int DEFAULT_PRELOAD_PAGES = 1;
	/**
	 * 若采取{@link ScrollPageType#MARGIN} 固定边距 或 {@link ScrollPageType#WIDTH}
	 * 固定宽度模式的话，预加载分页需要更改配置以适应显示。
	 */
	public static final int BACKUP_PRELOAD_PAGES = 2;

	/** 分页面 */
	private ScrollPage[] mPages;
	/** 当前页面索引 */
	private int mCurrentPage;
	/** 左右两侧预加载的分页数 */
	private int mPreloadPage = DEFAULT_PRELOAD_PAGES;
	/** 分页之间的间距 */
	private int mSpacing;
	/** 是否需要回弹效果，注：在{@link #mCircle}为true的状态下无效 */
	private boolean mBounces;
	/** 是否需要循环滚动效果 */
	private boolean mCircle;

	/** 当前滑动页面的模式选取 */
	private ScrollPageType mScrollPageType;
	/** 留边距离 */
	public int mMargin;
	/** 指定宽度 */
	private int mWidth;

	/**
	 * 滑动页面的模式
	 * 
	 * @see #FULL_SCREEN 全屏
	 * @see #MARGIN 固定留边距离
	 * @see #WIDTH 固定宽度
	 * @author XuWei
	 * 
	 */
	public enum ScrollPageType {
		FULL_SCREEN, MARGIN, WIDTH
	}

	/**
	 * 构造
	 * 
	 * @param pages
	 */
	public ScrollPageController(ScrollPage... pages) {
		this(ScrollPageType.FULL_SCREEN, pages);
	}

	/**
	 * 构造
	 * 
	 * @param type
	 *            指定滑动页面的模式
	 * @param pages
	 */
	public ScrollPageController(ScrollPageType type, ScrollPage... pages) {
		this(type, 0, pages);
	}

	/**
	 * 构造
	 * 
	 * @param type
	 *            指定滑动页面的模式
	 * @param value
	 *            对应模式下的赋值，如：margin的宽度或者指定的width
	 * @param pages
	 */
	public ScrollPageController(ScrollPageType type, int value,
			ScrollPage... pages) {
		mScrollPageType = type;
		if (type == ScrollPageType.MARGIN) {
			mMargin = value;
		} else if (type == ScrollPageType.WIDTH) {
			mWidth = value;
		}
		mPages = pages;
		for (ScrollPage page : pages)
			page.onCreate();
	}

	/**
	 * 获取预设的Pages
	 * 
	 * @return
	 */
	protected ScrollPage[] getPages() {
		return mPages;
	}

	/**
	 * resume状态，唤起当前页面
	 */
	public void resume() {
		List<Integer> runningList = getCurrentViews(mCurrentPage);
		for (int index : runningList) {
			mPages[index].start();
		}
	}

	/**
	 * pause状态，暂停当前页面
	 */
	public void pause() {
		List<Integer> runningList = getCurrentViews(mCurrentPage);
		for (int index : runningList) {
			mPages[index].pause();
		}
	}

	/**
	 * 销毁全部page
	 */
	public void destory() {
		for (ScrollPage page : mPages)
			page.onDestory();
	}

	/**
	 * 获取当前页面索引
	 * 
	 * @return
	 */
	public int getCurrentPage() {
		return mCurrentPage;
	}

	/**
	 * 设置当前页面
	 * 
	 * @param currentPage
	 */
	protected void setCurrentPage(int currentPage) {
		mCurrentPage = currentPage;

		List<Integer> runningList = getCurrentViews(currentPage);

		for (int i = 0; i < mPages.length; i++) {
			if (runningList.contains(i)) {
				mPages[i].start();
			} else {
				mPages[i].pause();
			}
		}
	}

	/**
	 * 获取当前索引下的左右激活view
	 * 
	 * @param current
	 * @return
	 */
	private List<Integer> getCurrentViews(int current) {
		LinkedList<Integer> runningList = new LinkedList<Integer>();

		runningList.add(current);
		for (int i = mPreloadPage - 1; i > 0; i--) {
			int left = mCircle ? ((current - i + mPages.length) % mPages.length)
					: (current - i);
			if (left >= 0) {
				runningList.add(left);
			}
		}

		for (int i = 1; i <= mPreloadPage - 1; i++) {
			int right = mCircle ? ((current + i) % mPages.length)
					: (current + i);
			if (right < mPages.length) {
				runningList.add(right);
			}
		}

		return runningList;
	}

	@Override
	public abstract void onInitSuccess();

	@Override
	public abstract void onUserScrollStart();

	@Override
	public abstract void onUserScrollEnd();

	@Override
	public abstract void onCurrentViewChanged(int currentView);

	@Override
	public abstract void onScroll(int currentView, float percent);

	/**
	 * 获取左右预加载分页数量
	 * 
	 * @return
	 */
	public int getPreloadPage() {
		return mPreloadPage;
	}

	/**
	 * 获取分页间距
	 * 
	 * @return
	 */
	public int getSpacing() {
		return mSpacing;
	}

	/**
	 * 是否需要回弹效果
	 * 
	 * @return
	 */
	public boolean isBounces() {
		return mBounces;
	}

	/**
	 * 是否循环滚动
	 * 
	 * @return
	 */
	public boolean isCircle() {
		return mCircle;
	}

	/**
	 * 获取留边距离
	 * 
	 * @return
	 */
	public int getMargin() {
		return mMargin;
	}

	/**
	 * 获取指定宽度
	 * 
	 * @return
	 */
	public int getWidth() {
		return mWidth;
	}

	/**
	 * 获取滑动页面的模式
	 * 
	 * @return
	 */
	public ScrollPageType getScrollPageType() {
		return mScrollPageType;
	}

	/**
	 * 设置左右预加载分页数量
	 * 
	 * @param preloadPage
	 */
	public void setPreloadPage(int preloadPage) {
		mPreloadPage = preloadPage;
	}

	/**
	 * 设置分页间距
	 * 
	 * @param spacing
	 */
	public void setSpacing(int spacing) {
		this.mSpacing = spacing;
	}

	/**
	 * 设置是否需要回弹
	 * 
	 * @param bounces
	 */
	public void setBounces(boolean bounces) {
		this.mBounces = bounces;
	}

	/**
	 * 设置是否循环滚动
	 * 
	 * @param circle
	 */
	public void setCircle(boolean circle) {
		this.mCircle = circle;
		if (mPages.length <= 1) {
			this.mCircle = false;
		}
	}

	/**
	 * 设置留边距离
	 * 
	 * @param mMargin
	 */
	public void setMargin(int mMargin) {
		this.mMargin = mMargin;
	}

	/**
	 * 设置指定宽度
	 * 
	 * @param mWidth
	 */
	public void setWidth(int mWidth) {
		this.mWidth = mWidth;
	}

	/**
	 * 根据分页View获取对应索引
	 * 
	 * @param view
	 * @return
	 */
	protected int getIndex(View view) {
		for (int i = 0; i < mPages.length; i++)
			if (mPages[i].getView() == view)
				return i;
		return 0;
	}
}
