package com.ifeng.util.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ifeng.util.ui.IIFAnimation.IFAnimation;

/**
 * 顶部滑动Tab栏自定义控件。预先调用{@link #setNormalTextColor}、{@link #setNormalTextSize}
 * 等配置方法后， 调用{@link #addTabsByTitles}添加标签栏内容。
 * 
 * @author XuWei 2013-5-27
 * 
 */
public class SlideTabbarView extends FrameLayout {

	/** 默认滑块滑动时间 */
	private int DEFAULT_SLIDE_TIME = 250;
	/** 默认Tab标题字色 */
	private int DEFAULI_TEXTCOLOR = Color.BLACK;
	/** 默认Tab标题字号 */
	private int DEFAULT_TEXTSIZE = 20;
	/** 默认资源id */
	private int DEFAULT_RES_ID = -1;
	/** 初始化标签 */
	private int DEFAULT_TAB = -1;

	/** 滑动时间 */
	private int mSlideTime = DEFAULT_SLIDE_TIME;
	/** 正常态Tab标题字色 */
	private int mNormalTextColor = DEFAULI_TEXTCOLOR;
	/** 激活态Tab标题字色 */
	private int mActiveTextColor = DEFAULI_TEXTCOLOR;
	/** 正常态Tab标题字号 */
	private int mNormalTextSize = DEFAULT_TEXTSIZE;
	/** 激活态Tab标题字号 */
	private int mActiveTextSize = DEFAULT_TEXTSIZE;

	/** 标签栏容器 */
	private HorizontalScrollView mHorizontalScrollView;
	/** Tab标题的容器 */
	private LinearLayout mTabContainerLayout;
	/** 背景色View */
	private View mBackgroundView;
	/** 滑块View */
	private View mHintView;
	/** 背景资源id */
	private int mBackgroundResId = DEFAULT_RES_ID;
	/** 滑块背景资源id */
	private int mHintResId = DEFAULT_RES_ID;

	/** Tab标题 */
	private String[] mTabTitles;
	/** Tab点击事件监听 */
	private OnTabSelectedListener mOnTabSelectedListener;
	/** 滑动动画 */
	private MovingAnimation mMovingAnimation;

	/** 屏幕宽度 */
	private int mWidth;
	/** 屏幕像素密度 */
	private float mScaledDensity;

	/** 当前选中标签 */
	private int mCurrent;
	/** 像素密度 */
	private float mDensity;

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public SlideTabbarView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if ((mMovingAnimation != null && !mMovingAnimation.isAnimationEnded())
				&& !changed) {
			return;
		}
		super.onLayout(changed, left, top, right, bottom);

		if (mTabTitles != null
				&& (mMovingAnimation == null || mMovingAnimation
						.isAnimationEnded())) {
			View targetView = mTabContainerLayout.getChildAt(mCurrent);
			mHintView.layout(targetView.getLeft(), targetView.getTop(),
					targetView.getRight(), targetView.getBottom());
			mHorizontalScrollView.smoothScrollTo(targetView.getLeft(), 0);
		}
	}

	/**
	 * 初始化基本参数及对象
	 */
	private void init() {
		DisplayMetrics dm = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay()
				.getMetrics(dm);
		mDensity = dm.density;

		mBackgroundView = new View(getContext());
		mBackgroundView.setLayoutParams(new LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT));

		mHintView = new View(getContext());
		mHintView.setLayoutParams(new LayoutParams(0, 0));

		mTabContainerLayout = new LinearLayout(getContext());
		mTabContainerLayout.setOrientation(LinearLayout.HORIZONTAL);
		mTabContainerLayout.setLayoutParams(new LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT));

		mHorizontalScrollView = new HorizontalScrollView(getContext());
		mHorizontalScrollView.setLayoutParams(new LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT));

		FrameLayout frameLayout = new FrameLayout(getContext());
		frameLayout.setLayoutParams(new LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT));

		mHorizontalScrollView.setHorizontalScrollBarEnabled(false);

		frameLayout.addView(mHintView);
		frameLayout.addView(mTabContainerLayout);

		mHorizontalScrollView.addView(frameLayout);

		// 注意addView的先后顺序，保持遮罩效果显示正常
		addView(mBackgroundView);
		addView(mHorizontalScrollView);

		final DisplayMetrics displayMetrics = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay()
				.getMetrics(displayMetrics);
		mWidth = dm.widthPixels;
		mScaledDensity = getResources().getDisplayMetrics().scaledDensity;
	}

	/**
	 * 根据分页创建标签栏，用于定栏目的标签，比如仅有2、3页时
	 * 
	 * @param listener
	 * @param titles
	 */
	public void addTabsByTabs(OnTabSelectedListener listener, String... titles) {
		addTabs((int) px2dip(mWidth / titles.length), listener, titles);
	}

	/**
	 * 根据分页标题创建标签栏，用于滑动的多栏目标签
	 * 
	 * @param tabWidth
	 * @param listener
	 * @param titles
	 */
	public void addTabsByTitles(int tabWidth, OnTabSelectedListener listener,
			String... titles) {
		addTabs(tabWidth, listener, titles);
	}

	/**
	 * 对应提供的标题进行初始化
	 * 
	 * @param tabWidth
	 *            分页宽度
	 * @param listener
	 *            {@link OnTabSelectedListener}
	 * @param titles
	 */
	private void addTabs(int tabWidth, OnTabSelectedListener listener,
			String... titles) {
		if (titles == null) {
			return;
		}

		mOnTabSelectedListener = listener;
		mTabTitles = titles;

		mTabContainerLayout.removeAllViews();
		int tabSize = titles.length;
		mTabContainerLayout.setWeightSum(tabSize);
		for (int i = 0; i < mTabTitles.length; i++) {
			String title = mTabTitles[i];
			TextView titleView = new TextView(getContext());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					dip2px(tabWidth), LinearLayout.LayoutParams.FILL_PARENT);
			params.gravity = Gravity.CENTER;
			titleView.setGravity(Gravity.CENTER);
			titleView.setLayoutParams(params);
			titleView.setText(title);
			titleView.setTag(i);
			titleView.setOnClickListener(mOnTabClickListener);
			titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mNormalTextSize);
			titleView.setTextColor(mNormalTextColor);
			mTabContainerLayout.addView(titleView);
		}

		mHintView.setLayoutParams(new LayoutParams(dip2px(tabWidth),
				FrameLayout.LayoutParams.FILL_PARENT));

		if (mBackgroundResId != DEFAULT_RES_ID) {
			mBackgroundView.setBackgroundResource(mBackgroundResId);
		}

		if (mHintResId != DEFAULT_RES_ID) {
			mHintView.setBackgroundResource(mHintResId);
		}

		moveToWhich(DEFAULT_TAB, true);
	}

	/**
	 * 选择选项卡
	 * 
	 * @param position
	 */
	public void setSelect(int position) {
		moveToWhich(Math.max(0, Math.min(position, mTabTitles.length - 1)),
				true);
	}

	/**
	 * 选择Tab标签
	 * 
	 * @param which
	 */
	private void moveToWhich(int which, boolean byUser) {
		if (which == mCurrent) {
			return;
		} else if (which == DEFAULT_TAB) {
			which = 0;
		}
		if (!byUser) {
			mOnTabSelectedListener.onSelected(which);
		} else {
			if (mMovingAnimation != null
					&& !mMovingAnimation.isAnimationEnded()) {
				mMovingAnimation.stopAnimation();
			}
			mMovingAnimation = new MovingAnimation(which);
			mMovingAnimation.startAnimation();
		}
	}

	/**
	 * tab被点击
	 */
	private OnSingleClickListener mOnTabClickListener = new OnSingleClickListener() {

		@Override
		public void onSingleClick(View v) {
			if ((mMovingAnimation != null && mMovingAnimation
					.isAnimationEnded())) {
				moveToWhich((Integer) v.getTag(), false);
			}
		}
	};

	/**
	 * 滑块位移动画
	 * 
	 * @author Calvin
	 * 
	 */
	private class MovingAnimation extends IFAnimation {

		/** 目标Tab标签View */
		private TextView mTargetView;
		/** 原Tab标签View */
		private TextView mOriginView;

		/** 目标Tab字号步长 */
		private float mTargetTextSize;
		/** 原Tab字号步长 */
		private float mOriginTextSize;
		/** 目标Tab字色步长 */
		private float[] mTargetTextColor;
		/** 原Tab字色步长 */
		private float[] mOriginTextColor;

		/** 目标滚动条位置 */
		private int mTargetScroll;
		/** 原滚动条位置 */
		private int mOriginScroll;

		public MovingAnimation(int target) {
			super(SlideTabbarView.this, mSlideTime);

			mOriginView = (TextView) mTabContainerLayout.getChildAt(mCurrent);
			mTargetView = (TextView) mTabContainerLayout.getChildAt(target);

			mOriginTextSize = mNormalTextSize;
			mTargetTextSize = mActiveTextSize;

			mOriginTextColor = new float[] { Color.red(mNormalTextColor),
					Color.green(mNormalTextColor), Color.blue(mNormalTextColor) };
			mTargetTextColor = new float[] { Color.red(mActiveTextColor),
					Color.green(mActiveTextColor), Color.blue(mActiveTextColor) };

			/*
			 * 若目标在左侧，且当前不完全处于屏幕可显示位置
			 */
			if (mTargetView.getLeft() < mHorizontalScrollView.getScrollX()) {
				mTargetScroll = mTargetView.getLeft();
			}
			/*
			 * 若目标在右侧，且当前不完全处于屏幕可显示位置
			 */
			else if (mTargetView.getRight() > mHorizontalScrollView
					.getScrollX() + mWidth) {
				mTargetScroll = mTargetView.getRight() - mWidth;
			} else {
				mTargetScroll = mHorizontalScrollView.getScrollX();
			}
			mOriginScroll = mHorizontalScrollView.getScrollX();

			mCurrent = target;
		}

		@Override
		public void applyTransformation(float percent) {
			int dx = (int) ((mTargetView.getLeft() - mOriginView.getLeft()) * percent);
			mHintView.layout(mOriginView.getLeft() + dx, mOriginView.getTop(),
					mOriginView.getRight() + dx, mOriginView.getBottom());

			// TextView的getTextSize()方法返回值为px单位，需要除以像素密度获取sp或dip单位
			// TextView的setTextSize()方法设置的为sp单位字号.
			// setTextSize(unit, size)方法可指定单位类型
			// TypedValue.COMPLEX_UNIT_PX : Pixels
			// TypedValue.COMPLEX_UNIT_SP : Scaled Pixels
			// TypedValue.COMPLEX_UNIT_DIP : Device Independent
			// Pixels
			mOriginView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
					mTargetTextSize - (mTargetTextSize - mOriginTextSize)
							* percent);
			mTargetView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
					mOriginTextSize + (mTargetTextSize - mOriginTextSize)
							* percent);

			int originColor = Color
					.rgb((int) (mTargetTextColor[0] - (mTargetTextColor[0] - mOriginTextColor[0])
							* percent),
							(int) (mTargetTextColor[1] - (mTargetTextColor[1] - mOriginTextColor[1])
									* percent),
							(int) (mTargetTextColor[2] - (mTargetTextColor[2] - mOriginTextColor[2])
									* percent));
			int targetColor = Color
					.rgb((int) (mOriginTextColor[0] + (mTargetTextColor[0] - mOriginTextColor[0])
							* percent),
							(int) (mOriginTextColor[1] + (mTargetTextColor[1] - mOriginTextColor[1])
									* percent),
							(int) (mOriginTextColor[2] + (mTargetTextColor[2] - mOriginTextColor[2])
									* percent));
			mOriginView.setTextColor(originColor);
			mTargetView.setTextColor(targetColor);

			mHorizontalScrollView.smoothScrollBy(
					(int) (mOriginScroll + (mTargetScroll - mOriginScroll)
							* percent), 0);
		}

		@Override
		public void onAnimationFinished() {

		}

	}

	/**
	 * 设置滑块滑动时间
	 * 
	 * @param slideTime
	 */
	public void setSlideTime(int slideTime) {
		mSlideTime = slideTime;
	}

	/**
	 * 设置Tab栏背景。 注：调用在addTabsByTitles之前
	 * 
	 * @param resid
	 */
	public void setBackground(int resid) {
		mBackgroundResId = resid;
		if (mBackgroundView != null) {
			mBackgroundView.setBackgroundResource(resid);
		}
	}

	/**
	 * 设置滑块背景。 注：调用在addTabsByTitles之前
	 * 
	 * @param resid
	 */
	public void setHintBackground(int resid) {
		mHintResId = resid;
		if (mHintView != null) {
			mHintView.setBackgroundResource(resid);
		}
	}

	/**
	 * 设置标准态的标题字色。 注：调用在addTabsByTitles之前
	 * 
	 * @param r
	 * @param g
	 * @param b
	 */
	public void setNormalTextColor(int r, int g, int b) {
		int color = Color.rgb(r, g, b);
		// 若未设置特殊的激活态字色，则将其一并修改
		if (mNormalTextColor == DEFAULI_TEXTCOLOR) {
			mActiveTextColor = color;
		}
		mNormalTextColor = color;
	}

	/**
	 * 设置标准态的标题字色。 注：调用在addTabsByTitles之前
	 * 
	 * @param resId
	 */
	public void setNormalTextColor(int resId) {
		int color = getResources().getColor(resId);
		// 若未设置特殊的激活态字色，则将其一并修改
		if (mNormalTextColor == DEFAULI_TEXTCOLOR) {
			mActiveTextColor = color;
		}
		mNormalTextColor = color;
	}

	/**
	 * 设置激活态的标题字色。 注：调用在addTabsByTitles之前
	 * 
	 * @param r
	 * @param g
	 * @param b
	 */
	public void setActiveTextColor(int r, int g, int b) {
		int color = Color.rgb(r, g, b);
		mActiveTextColor = color;
	}

	/**
	 * 设置激活态的标题字色。 注：调用在addTabsByTitles之前
	 * 
	 * @param resId
	 */
	public void setActiveTextColor(int resId) {
		int color = getResources().getColor(resId);
		mActiveTextColor = color;
	}

	/**
	 * 设置标准态字号。 注：调用在addTabsByTitles之前
	 * 
	 * @param size
	 */
	public void setNormalTextSize(int size) {
		// 若未设置特殊的激活态字号，则将其一并修改
		if (mNormalTextSize == DEFAULT_TEXTSIZE) {
			mActiveTextSize = size;
		}
		mNormalTextSize = size;
	}

	/**
	 * 从Dimen中设置标准态字号。 注：调用在addTabsByTitles之前
	 * 
	 * @param size
	 */
	public void setNormalTextSizeFromDimen(int size) {
		setNormalTextSize((int) px2dip(size));
	}

	/**
	 * 提供对当前分辨率下dip px的换算
	 * 
	 * @param dip
	 * @return
	 */
	private int dip2px(int dip) {
		return (int) (dip * mDensity);
	}

	/**
	 * 提供对当前分辨率下px dip的换算
	 * 
	 * @param px
	 * @return
	 */
	private float px2dip(float px) {
		return (px / mDensity);
	}

	/**
	 * 设置激活态字号。 注：调用在addTabsByTitles之前，需先设置{@link #setNormalTextSize(int)}
	 * 
	 * @param size
	 */
	public void setActiveTextSize(int size) {
		mActiveTextSize = size;
	}

	/**
	 * 从Dimen中设置激活态字号。 注：调用在addTabsByTitles之前，需先设置
	 * {@link #setNormalTextSize(int)}
	 * 
	 * @param size
	 */
	public void setActiveTextSizeFromDimen(int size) {
		setActiveTextSize((int) px2dip(size));
	}

	/**
	 * Tab点击事件监听类
	 * 
	 * @author XuWei
	 * 
	 */
	public interface OnTabSelectedListener {
		/**
		 * 选择对应的标签时
		 * 
		 * @param which
		 */
		public void onSelected(int which);
	}
}
