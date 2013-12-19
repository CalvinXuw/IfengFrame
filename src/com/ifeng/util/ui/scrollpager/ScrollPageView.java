package com.ifeng.util.ui.scrollpager;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Scroller;

import com.ifeng.util.ui.scrollpager.ScrollPageController.ScrollPageType;

/**
 * 滑动翻页控件，使用时需要添加一个ScrollPageController控制器
 * 
 * @see ScrollPageController#setScrollPageController
 * @see ScrollPageController
 * @author xuwei
 * 
 */
public class ScrollPageView extends ViewGroup {

	/** 轻扫监听的最小时间间隔 */
	private final static int SWAP_MIN_TIME = 300;
	/** 轻扫监听的最小距离间隔 */
	private final static int SWAP_MIN_DISTANCE = 50;

	/** 当前显示的分页列表 */
	private LinkedList<View> mCurrentPages;
	/** 当前分页索引 */
	private int mCurrentPage;
	/** 滑动结束时即将回弹到的位置 */
	private View mNextPage;

	/** Scroller */
	private Scroller mScroller;
	/** 屏幕宽度 */
	private int mScreenWidth;
	/** View宽度 */
	private int mWidth;
	/** */
	private int mPageMargin;
	/** 捕捉到的滑动距离 */
	private int mScrollDistance;
	/** 上一次触摸事件的X坐标 */
	private float mLastMotionX;
	/** 上一次触摸事件的Y坐标 */
	private float mLastMotionY;
	/** 滑动事件的按下时间记录 */
	private long mDownTime;
	/** 滑动事件的抬起时间记录 */
	private long mUpTime;

	/** 是否首次布局 */
	private boolean mIsFirstLayout;
	/** 是否已经捕捉事件 */
	private boolean mIsDispatched;
	/** 是否是滑动页面事件 */
	private boolean mIsScrollEvent;
	/** 是否取消了尚未Cancel的事件 */
	private boolean mIsCancelOthers;
	/** 是否已经停止拦截事件 */
	private boolean mIsDropTouchEvent;

	/** 滑动页面的控制器 */
	private ScrollPageController mScrollPageController;

	/**
	 * 构造
	 * 
	 * @param context
	 */
	public ScrollPageView(Context context) {
		super(context);
	}

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public ScrollPageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public ScrollPageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * 初始化
	 */
	private void init() {
		mScroller = new Scroller(getContext());
		mScreenWidth = mWidth = getWidth();
		if (mScrollPageController.getScrollPageType() == ScrollPageType.MARGIN) {
			/*
			 * 对页面宽度进行保护，如果margin设定过大导致页面宽度为0，则该设定无效
			 */
			int margin = mScrollPageController.getMargin() * 2;
			if (mWidth - margin > 0) {
				mWidth -= margin;
			}
		} else if (mScrollPageController.getScrollPageType() == ScrollPageType.WIDTH) {
			/*
			 * 对页面宽度进行保护，如果指定的width长度大于屏幕宽度或本身宽度为0，则该设定无效
			 */
			if (mScrollPageController.getWidth() <= mWidth
					&& mScrollPageController.getWidth() > 0) {
				mWidth = mScrollPageController.getWidth();
			}
		}
		mPageMargin = (mScreenWidth - mWidth) / 2;

		/*
		 * 修正预加载分页数，如果当前非全屏模式且预加载分页数为默认值的话需要进行修正。
		 */
		if (mScrollPageController.getScrollPageType() != ScrollPageType.FULL_SCREEN
				&& mScrollPageController.getPreloadPage() == ScrollPageController.DEFAULT_PRELOAD_PAGES) {
			mScrollPageController
					.setPreloadPage(ScrollPageController.BACKUP_PRELOAD_PAGES);
		}

		mIsFirstLayout = true;
		new Handler(getContext().getMainLooper()).postDelayed(new Runnable() {

			@Override
			public void run() {
				mScrollPageController.onInitSuccess();
			}
		}, 100);
	}

	/**
	 * 设置一个滑动页面控制器，从中获取相应的配置参数及页面信息。如：是否循环滚动 是否需要回弹效果、以及页面间距和页面大小本身的配置。
	 * 
	 * @param scrollPageController
	 */
	public void setScrollPageController(
			ScrollPageController scrollPageController) {
		if (scrollPageController == null)
			return;
		mScrollPageController = scrollPageController;

		if (getMeasuredWidth() != 0 && !mIsFirstLayout) {
			init();
		}
	}

	/**
	 * 以指定的页面索引进行初始化页面操作
	 * 
	 * @param initViewIndex
	 */
	public void startWithView(int initViewIndex) {
		mCurrentPage = initViewIndex;
		getCurrentPages();
		resetCurrentPages();
		mScrollPageController.setCurrentPage(initViewIndex);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (mScrollPageController == null)
			return;

		for (int i = 0; i < getChildCount(); i++) {
			View view = getChildAt(i);
			view.layout(view.getLeft(), view.getTop(), view.getRight(),
					view.getBottom());
		}

		if (!mIsFirstLayout) {
			init();
		}
	}

	/**
	 * 根据当前页面索引及预加载分页数构建分页列表
	 */
	private void getCurrentPages() {
		mCurrentPages = new LinkedList<View>();
		ScrollPage[] pages = mScrollPageController.getPages();
		int size = pages.length;

		/*
		 * 若当前仅有一页
		 */
		if (size == 1) {
			mCurrentPages.add(pages[0].getView());
			return;
		}

		/*
		 * 若当前分页总数大于需要加载的页数
		 */
		if (size >= (mScrollPageController.getPreloadPage() * 2 + 1)) {
			for (int i = mScrollPageController.getPreloadPage(); i > 0; i--) {
				/*
				 * 若循环，例如：4 5 0 1 2，或 0 1 2 3 4 . 若非循环，则不填满单侧预加载分页
				 */
				int pageIndex = mScrollPageController.isCircle() ? (mCurrentPage
						- i + size)
						% size
						: (mCurrentPage - i);
				if (pageIndex >= 0) {
					mCurrentPages.add(pages[pageIndex].getView());
				}
			}

			mCurrentPages.add(pages[mCurrentPage].getView());

			for (int i = 1; i <= mScrollPageController.getPreloadPage(); i++) {
				int pageIndex = mScrollPageController.isCircle() ? (mCurrentPage + i)
						% size
						: (mCurrentPage + i);
				if (pageIndex < size) {
					mCurrentPages.add(pages[pageIndex].getView());
				}
			}
		}

		/*
		 * 若当前分页总数小于需要加载的页数
		 */
		if (size < (mScrollPageController.getPreloadPage() * 2 + 1)) {
			for (int i = mScrollPageController.getPreloadPage(); i > 0; i--) {
				/*
				 * 若循环，例如：4 5 0 1 2，或 0 1 2 3 4 . 若非循环，则不填满单侧预加载分页
				 */
				int pageIndex = mScrollPageController.isCircle() ? (mCurrentPage
						- i + size)
						% size
						: (mCurrentPage - i);
				if (pageIndex >= 0) {
					/*
					 * 若左侧索引序列在当前索引项之后或等同，则创建ShadowView影像来填充
					 */
					if (pageIndex >= mCurrentPage) {
						/*
						 * 当前页面左侧的分页具有优先加载的权限，但当左侧页面和右侧页面相同时，优先右侧页面进行加载.
						 */
						if (i == 1 && pageIndex != ((mCurrentPage + 1) % size)) {
							mCurrentPages.add(pages[pageIndex].getView());
						} else {
							mCurrentPages.add(new ShadowView(getContext(),
									pages[pageIndex].getView()));
						}
					} else {
						mCurrentPages.add(pages[pageIndex].getView());
					}
				}
			}

			mCurrentPages.add(pages[mCurrentPage].getView());

			for (int i = 1; i <= mScrollPageController.getPreloadPage(); i++) {
				int pageIndex = mScrollPageController.isCircle() ? (mCurrentPage + i)
						% size
						: (mCurrentPage + i);
				if (pageIndex < size) {
					/*
					 * 若右侧索引序列在当前索引项之前或等同，则创建ShadowView影像来填充
					 */
					if (pageIndex <= mCurrentPage) {
						mCurrentPages.add(new ShadowView(getContext(),
								pages[pageIndex].getView()));
					} else {
						/*
						 * 若已经作为真实页面进行过添加，则添加ShadowView进行填充
						 */
						if (mCurrentPages.contains(pages[pageIndex].getView())) {
							mCurrentPages.add(new ShadowView(getContext(),
									pages[pageIndex].getView()));
						} else {
							mCurrentPages.add(pages[pageIndex].getView());
						}
					}
				}
			}
		}
	}

	/**
	 * 重新布局当前需要展示的页面
	 */
	private void resetCurrentPages() {
		removeAllViews();
		for (int i = 0; i < mCurrentPages.size(); i++) {
			int left = i * (mWidth + mScrollPageController.getSpacing());
			int viewWidth = mWidth;
			int viewHeight = getMeasuredHeight();

			mCurrentPages.get(i).setLayoutParams(
					new LayoutParams(mWidth, getHeight()));
			addView(mCurrentPages.get(i));
			mCurrentPages.get(i).layout(left, 0, left + viewWidth, viewHeight);

			if (i != mCurrentPages.size() - 1) {
				SpaceView spaceView = new SpaceView(getContext());
				addView(spaceView);
				spaceView.layout(left + viewWidth, 0, left + viewWidth
						+ mScrollPageController.getSpacing(), viewHeight);
			}
		}

		if (mNextPage != null)
			scrollTo(mNextPage.getLeft() - mPageMargin, getScrollY());
		else
			scrollTo(mScrollPageController.getPages()[mCurrentPage].getView()
					.getLeft() - mPageMargin, getScrollY());

		for (View view : mCurrentPages)
			if (view instanceof ShadowView)
				((ShadowView) view).getScreenShot();
	}

	@Override
	public void computeScroll() {
		if (mScroller == null) {
			return;
		}
		if (mScroller.computeScrollOffset()) {
			scrollBy(mScroller.getCurrX() - getScrollX(), mScroller.getCurrY()
					- getScrollY());
			invalidate();
		} else if (mNextPage != null) {

			/*
			 * 监听页面滑动
			 */
			View currentView = mScrollPageController.getPages()[mCurrentPage]
					.getView();
			View nextView = mNextPage instanceof ShadowView ? ((ShadowView) mNextPage)
					.getOriginView() : mNextPage;

			if (mNextPage instanceof ShadowView) {
				mCurrentPage = mScrollPageController
						.getIndex(((ShadowView) mNextPage).getOriginView());
				((ShadowView) mNextPage).clearScreenShot();
				mNextPage = ((ShadowView) mNextPage).getOriginView();
			} else {
				mCurrentPage = mScrollPageController.getIndex(mNextPage);
			}

			/*
			 * 重置
			 */
			if (currentView != nextView) {
				mScrollPageController
						.onCurrentViewChanged(mScrollPageController
								.getIndex(nextView));

				getCurrentPages();
				resetCurrentPages();
			}

			mNextPage = null;
		}

		float percent = (float) (getScrollX() - mScrollPageController
				.getPages()[mCurrentPage].getView().getLeft())
				/ (mWidth + mScrollPageController.getSpacing());
		if (!mScrollPageController.isCircle()
				&& ((mCurrentPage == 0 && percent < 0) || (mCurrentPage == mScrollPageController
						.getPages().length - 1 && percent > 0))) {
			return;
		}
		mScrollPageController.onScroll(mCurrentPage, percent);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			mIsDropTouchEvent = false;
		}

		if (mIsDropTouchEvent && mIsDispatched) {
			getParent().requestDisallowInterceptTouchEvent(false);
			return false;
		}
		return super.dispatchTouchEvent(ev);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		/*
		 * 为了适配ScrollPagerView的嵌套情况的正常使用，在子视图中屏蔽到父视图的事件拦截
		 */
		getParent().requestDisallowInterceptTouchEvent(true);
		if (mScrollPageController == null)
			return super.onInterceptTouchEvent(ev);
		switch (ev.getAction()) {
		/*
		 * 记录Down事件起始信息
		 */
		case MotionEvent.ACTION_DOWN:
			mIsCancelOthers = false;
			mIsScrollEvent = false;
			mLastMotionY = ev.getY();
			mLastMotionX = ev.getX();
			mIsDispatched = true;
			onTouchEvent(ev);
			return super.onInterceptTouchEvent(ev);

		case MotionEvent.ACTION_MOVE:
			/*
			 * 第一次Move时，判断移动方向，只有横向的移动才交由本Gallery处理
			 */
			if (mIsDispatched) {

				float x;
				float y;

				int n = ev.getHistorySize();
				if (n != 0) {
					x = ev.getHistoricalX(n - 1);
					y = ev.getHistoricalY(n - 1);
				} else {
					x = ev.getX();
					y = ev.getY();
				}

				float deltaX = x - mLastMotionX;
				float deltaY = y - mLastMotionY;
				deltaX = (deltaX > 0) ? deltaX : -deltaX;
				deltaY = (deltaY > 0) ? deltaY : -deltaY;

				if (deltaX - deltaY > 5 && !mIsCancelOthers) {
					mIsCancelOthers = true;
					MotionEvent mCancleEvent = MotionEvent.obtain(
							ev.getDownTime(), ev.getEventTime(),
							MotionEvent.ACTION_CANCEL, ev.getX(), ev.getY(),
							ev.getMetaState());
					performCancelEvent(this, mCancleEvent);
					mCancleEvent.recycle();
				}
				// 比较纵向移动距离和横向移动距离，以判断移动方向
				// 理论上，其差>0 即可判断。然，需要考虑手指在屏幕上的微颤
				// 因为手指会产生一些微小的不符合用户预期方向的移动，所以改为 >15，以过滤这些移动
				if (deltaY - deltaX > 15) {
					mIsScrollEvent = false;
					mIsDispatched = false;
					try {
						return super.onInterceptTouchEvent(ev);
					} catch (Exception e) {
						return false;
					}
				} else if (deltaX - deltaY > 15) {
					if (mScrollPageController != null)
						mScrollPageController.onUserScrollStart();
					onTouchEvent(ev);
					mIsScrollEvent = true;
					mIsDispatched = false;
					return true;
				}
			}
		default:
			/*
			 * 根据Down和Move事件的判断，决定交给谁来处理
			 */
			if (mIsScrollEvent) {
				/*
				 * 交给自己
				 */
				onTouchEvent(ev);
				return true;
			} else {
				/*
				 * 交给子控件
				 */
				try {
					return super.onInterceptTouchEvent(ev);
				} catch (Exception e) {
					return false;
				}
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		final float x = event.getX();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mDownTime = System.currentTimeMillis();
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}
			mLastMotionX = x;
			mScrollDistance = 0;
			break;
		case MotionEvent.ACTION_MOVE:
			int deltaX = (int) (mLastMotionX - x);
			mLastMotionX = x;
			mScrollDistance += deltaX;
			scrollBy(deltaX, 0);

			if (!mScrollPageController.isBounces()
					&& !mScrollPageController.isCircle()) {
				int scrollX = getScrollX() + mScrollPageController.getMargin();
				if ((scrollX <= 0 && deltaX < 0)
						|| (scrollX >= getChildAt(getChildCount() - 1)
								.getLeft() && deltaX > 0)) {
					mIsDropTouchEvent = true;
				}
			}
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if (mScrollPageController != null)
				mScrollPageController.onUserScrollEnd();
			mUpTime = System.currentTimeMillis();
			snapToDestination();
			break;
		}
		return true;
	}

	/**
	 * 根据捕获的事件及滑动距离计算目标分页
	 */
	private void snapToDestination() {
		int currentInPages = mCurrentPages.indexOf(mScrollPageController
				.getPages()[mCurrentPage].getView());

		/*
		 * 快速划过
		 */
		if (mUpTime - mDownTime < SWAP_MIN_TIME
				&& Math.abs(mScrollDistance) > SWAP_MIN_DISTANCE) {
			if (mScrollDistance > 0)
				snapToScreen(mCurrentPages.get(Math.min(currentInPages + 1,
						mCurrentPages.size() - 1)));
			else if (mScrollDistance < 0)
				snapToScreen(mCurrentPages.get(Math.max(currentInPages - 1, 0)));
			return;
		}

		if (Math.abs(mScrollDistance) > mWidth / 2) {
			if (mScrollDistance > 0)
				snapToScreen(mCurrentPages.get(Math.min(currentInPages + 1,
						mCurrentPages.size() - 1)));
			else if (mScrollDistance < 0)
				snapToScreen(mCurrentPages.get(Math.max(currentInPages - 1, 0)));
		} else
			snapToScreen(mCurrentPages.get(currentInPages));
	}

	/**
	 * 滑动到指定分页
	 * 
	 * @param whichPage
	 */
	public void snapToScreen(View whichPage) {
		if (!mScroller.isFinished())
			return;

		View currentPageView = mScrollPageController.getPages()[mCurrentPage]
				.getView();

		boolean changingScreens = whichPage != currentPageView;

		mNextPage = whichPage;
		if (mNextPage instanceof ShadowView)
			mScrollPageController.setCurrentPage(mScrollPageController
					.getIndex(((ShadowView) mNextPage).getOriginView()));
		else
			mScrollPageController.setCurrentPage(mScrollPageController
					.getIndex(mNextPage));

		View focusedChild = getFocusedChild();
		if (focusedChild != null && changingScreens
				&& focusedChild == currentPageView)
			focusedChild.clearFocus();

		/*
		 * 让mScroller启动滚动
		 */
		final int cx = getScrollX();
		final int newX = whichPage.getLeft() - mPageMargin;
		final int delta = newX - cx;
		mScroller.startScroll(cx, 0, delta, 0,
				Math.max(mWidth, Math.abs(delta) * 2));

		invalidate();
	}

	@Override
	public void scrollBy(int x, int y) {
		int scrollX = getScrollX() + mScrollPageController.getMargin();
		if (scrollX + x <= 0) {
			if (mScrollPageController.isBounces())
				x = x / 3;
			else
				x = -getScrollX() - mScrollPageController.getMargin();
		} else if (scrollX + x >= getChildAt(getChildCount() - 1).getLeft()) {
			if (mScrollPageController.isBounces())
				x = x / 3;
			else
				x = getChildAt(getChildCount() - 1).getLeft() - scrollX;
		}

		super.scrollBy(x, y);
	}

	/**
	 * 模拟取消事件，完结被其余View捕捉到的touch事件
	 * 
	 * @param group
	 * @param me
	 */
	private void performCancelEvent(ViewGroup group, MotionEvent me) {
		final int count = group.getChildCount();
		for (int i = 0; i < count; i++) {
			View childView = group.getChildAt(i);
			childView.onTouchEvent(me);
			if (childView instanceof ViewGroup)
				performCancelEvent((ViewGroup) childView, me);
		}
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
	 * 在滑动分页之间的间距填充View
	 * 
	 * @author xuwei
	 * 
	 */
	private class SpaceView extends ImageView {

		public SpaceView(Context context) {
			super(context);
			setBackgroundColor(Color.GRAY);
		}

	}

	/**
	 * 用于辅助实现少量页面的循环翻页功能
	 * 
	 * @author xuwei
	 * 
	 */
	private class ShadowView extends ImageView {

		/** 源View */
		private View mOriginView;

		/**
		 * 构造
		 * 
		 * @param context
		 * @param originView
		 */
		public ShadowView(Context context, View originView) {
			super(context);
			mOriginView = originView;
		}

		/**
		 * 清除图片缓存
		 */
		public void clearScreenShot() {
			setImageBitmap(null);
			// System.gc();
		}

		/**
		 * 从源View中截取图像投影到ShadowView上
		 */
		public void getScreenShot() {
			mOriginView.destroyDrawingCache();
			mOriginView.setDrawingCacheEnabled(true);
			if (mOriginView.getDrawingCache(true) != null) {
				Bitmap bm = Bitmap.createBitmap(mOriginView
						.getDrawingCache(true));
				setImageBitmap(bm);
				mOriginView.setDrawingCacheEnabled(false);
			}
		}

		/**
		 * 获取源View
		 * 
		 * @return
		 */
		public View getOriginView() {
			return mOriginView;
		}
	}

	/**
	 * 滑动页面的事件触发监听
	 * 
	 * @author xuwei
	 * 
	 */
	public interface ScrollPageListener {

		/**
		 * 初始化完成
		 */
		public void onInitSuccess();

		/**
		 * 用户开始滑动页面
		 */
		public void onUserScrollStart();

		/**
		 * 用户松开停止滑动
		 */
		public void onUserScrollEnd();

		/**
		 * 当前页面索引发生变化
		 * 
		 * @param currentView
		 */
		public void onCurrentViewChanged(int currentView);

		/**
		 * 滑动事件触发
		 * 
		 * @param currentView
		 * @param percent
		 */
		public void onScroll(int currentView, float percent);

	}
}
