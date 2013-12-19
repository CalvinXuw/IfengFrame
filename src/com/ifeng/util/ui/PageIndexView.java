package com.ifeng.util.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ifeng.android.R;
import com.ifeng.util.ui.IIFAnimation.IFAnimation;

/**
 * 页卡索引控件
 * 
 * @author Calvin
 * 
 */
public class PageIndexView extends RelativeLayout {

	/** 提示块宽度 */
	private static final int HINTER_WIDTH = 35;
	/** 提示块高度 */
	private static final int HINTER_HEIGHT = 40;
	/** 滑块宽度 */
	private static final int SLIDER_WIDTH = 25;
	/** 滑块高度 */
	private static final int SLIDER_HEIGHT = 15;
	/** 左右边距 */
	private static final int MARGIN = 55;
	/** 滑块字体大小 */
	private static final int SLIDER_TEXT_SIZE = 10;
	/** 提示字体大小 */
	private static final int HINTER_TEXT_SIZE = 15;

	/** 像素密度 */
	private float mDensity;
	/** 屏幕宽度 */
	private int mWidth;
	/** 滑块组 */
	private RelativeLayout mSliderGroup;
	/** 滑块view */
	private TextView mSlider;
	/** 提示块view */
	private TextView mHinter;

	/** 滑动动画 */
	private MovingAnimation mMovingAnimation;

	/** 是否经过了初始化 */
	private boolean mIsInit;
	/** 设置最大索引值 */
	private int mMax;
	/** 当前页码 */
	private int mCurrent;
	/** callback */
	private OnPageIndexSelected mOnPageIndexSelected;

	public PageIndexView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * 初始化
	 */
	private void init() {
		DisplayMetrics dm = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay()
				.getMetrics(dm);
		mDensity = dm.density;
		mWidth = dm.widthPixels;

		mSliderGroup = new RelativeLayout(getContext());
		LayoutParams sliderGrouParams = new LayoutParams(
				LayoutParams.FILL_PARENT, dip2px(SLIDER_HEIGHT));
		sliderGrouParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		sliderGrouParams.leftMargin = dip2px((HINTER_WIDTH - SLIDER_WIDTH) / 2);
		sliderGrouParams.rightMargin = dip2px((HINTER_WIDTH - SLIDER_WIDTH) / 2);
		mSliderGroup.setLayoutParams(sliderGrouParams);

		// 在滑块viewgroup中间添加分割线
		View splitline = new View(getContext());
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT,
				dip2px(1));
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		splitline.setBackgroundColor(getResources().getColor(R.color.divider));
		splitline.setLayoutParams(params);
		mSliderGroup.addView(splitline);

		// 添加滑块
		mSlider = new TextView(getContext());
		mSlider.setLayoutParams(new LayoutParams(dip2px(SLIDER_WIDTH),
				dip2px(SLIDER_HEIGHT)));
		mSlider.setBackgroundColor(getResources().getColor(
				R.color.pageindex_slider));
		mSlider.setGravity(Gravity.CENTER);
		mSlider.setTextColor(getResources().getColor(R.color.font_list_cover));
		mSlider.setTextSize(TypedValue.COMPLEX_UNIT_DIP, SLIDER_TEXT_SIZE);
		mSlider.setOnTouchListener(mSliderOnTouchListener);
		mSliderGroup.addView(mSlider);

		addView(mSliderGroup);

		// hinter
		mHinter = new TextView(getContext());
		mHinter.setLayoutParams(new LayoutParams(dip2px(HINTER_WIDTH),
				dip2px(HINTER_HEIGHT)));
		mHinter.setBackgroundResource(R.drawable.image_pageindex_hint);
		mHinter.setTextColor(getResources().getColor(R.color.font_list_cover));
		mHinter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, HINTER_TEXT_SIZE);
		mHinter.setPadding(0, 0, 0, dip2px(8));
		mHinter.setGravity(Gravity.CENTER);
		mHinter.setVisibility(View.INVISIBLE);

		addView(mHinter);
	}

	/** 滑块拖动事件监听 */
	private OnTouchListener mSliderOnTouchListener = new OnTouchListener() {

		/** touch坐标记录 */
		private float mLastX;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mHinter.setVisibility(View.VISIBLE);
				mLastX = event.getX();
				moveHinter();
				mSlider.setBackgroundColor(getResources().getColor(
						R.color.divider));
				mSlider.setText("");
				break;
			case MotionEvent.ACTION_MOVE:
				float dx = event.getX() - mLastX;
				moveSlider(dx);
				moveHinter();
				break;
			case MotionEvent.ACTION_UP:
				mHinter.setVisibility(View.INVISIBLE);
				mSlider.setBackgroundColor(getResources().getColor(
						R.color.pageindex_slider));
				if (mOnPageIndexSelected != null) {
					mOnPageIndexSelected.onSelected(getIndexByX(mSlider
							.getLeft()));
				}
				setCurrent(getIndexByX(mSlider.getLeft()));
				break;
			}
			return true;
		}

		/**
		 * touch事件中位移滑块
		 * 
		 * @param dx
		 */
		private void moveSlider(float dx) {
			int min = 0;
			int max = (int) ((mMax - 1) * getDistanceStep()) + (mMax - 1)
					* dip2px(SLIDER_WIDTH);

			int newLeft = (int) Math.max(min,
					Math.min(max, mSlider.getLeft() + dx));

			mSlider.layout(newLeft, 0, newLeft + dip2px(SLIDER_WIDTH),
					dip2px(SLIDER_HEIGHT));
			mHinter.setText(getIndexByX(newLeft) + 1 + "");
		}

		/**
		 * touch事件中位移提示块
		 */
		private void moveHinter() {
			int left = mSlider.getLeft();
			mHinter.layout(left, 0, left + dip2px(HINTER_WIDTH),
					dip2px(HINTER_HEIGHT));
		}

		/**
		 * 根据当前位移获取索引值
		 * 
		 * @param x
		 * @return
		 */
		private int getIndexByX(int x) {
			for (int i = 0; i < mMax; i++) {
				int left = (int) (i * getDistanceStep()) + i
						* dip2px(SLIDER_WIDTH);
				if (x < left) {
					return i - 1;
				} else if (i == mMax - 1) {
					return mMax - 1;
				}
			}
			return 0;
		}
	};

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (mMovingAnimation == null || mMovingAnimation.isAnimationEnded()) {
			super.onLayout(changed, l, t, r, b);
			if (mIsInit) {
				setCurrent(mCurrent, false);
			}
		}
	}

	/**
	 * 设置当前最大索引值
	 * 
	 * @param max
	 */
	public void setMax(int max) {
		if (!mIsInit) {
			mIsInit = true;
			init();
		}
		mMax = max;
		setCurrent(mCurrent, false);
	}

	/**
	 * 设置当前索引值
	 * 
	 * @param current
	 */
	public void setCurrent(int current) {
		current = Math.max(0, Math.min(current, mMax - 1));
		setCurrent(current, true);
	}

	/**
	 * 加入监听回调
	 * 
	 * @param callback
	 */
	public void setOnPageIndexSelected(OnPageIndexSelected callback) {
		mOnPageIndexSelected = callback;
	}

	/**
	 * 设置当前索引值，私有，可控是否进行动画
	 * 
	 * @param current
	 * @param isAnim
	 */
	private void setCurrent(int current, boolean isAnim) {
		mSlider.setText(current + 1 + "");
		if (!isAnim) {
			int left = (int) (current * getDistanceStep()) + current
					* dip2px(SLIDER_WIDTH);
			mSlider.layout(left, 0, left + dip2px(SLIDER_WIDTH),
					dip2px(SLIDER_HEIGHT));
			mCurrent = current;
		} else {
			mMovingAnimation = new MovingAnimation(current);
			mMovingAnimation.startAnimation();
		}
	}

	/**
	 * 是否完成初始化
	 * 
	 * @return
	 */
	public boolean isInit() {
		return mIsInit;
	}

	private class MovingAnimation extends IFAnimation {

		/** 默认滑块滑动时间 */
		private static final int DEFAULT_SLIDE_TIME = 100;

		/** 起始位置 */
		private int mStartLeft;
		/** 距离步长 */
		private float mDistanceStep;

		/**
		 * 构造
		 * 
		 * @param target
		 */
		public MovingAnimation(int target) {
			super(PageIndexView.this, DEFAULT_SLIDE_TIME);

			int left = (int) (target * getDistanceStep()) + target
					* dip2px(SLIDER_WIDTH);

			mDistanceStep = left - mSlider.getLeft();
			mStartLeft = mSlider.getLeft();

			mCurrent = target;
		}

		@Override
		public void applyTransformation(float percent) {
			int newLeft = (int) (mStartLeft + mDistanceStep * percent);
			mSlider.layout(newLeft, 0, newLeft + dip2px(SLIDER_WIDTH),
					dip2px(SLIDER_HEIGHT));
		}

		@Override
		public void onAnimationFinished() {
			setCurrent(mCurrent, false);
		}

	}

	/**
	 * 计算滑块滑动步长
	 * 
	 * @return
	 */
	private float getDistanceStep() {
		int leftMargin = dip2px(MARGIN);
		int rightMargin = dip2px(MARGIN);

		return (mWidth - dip2px(HINTER_WIDTH - SLIDER_WIDTH) - leftMargin
				- rightMargin - mMax * dip2px(SLIDER_WIDTH))
				/ (float) (mMax - 1);
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
	 * 获取滑动页卡索引的事件回调
	 * 
	 * @author Calvin
	 * 
	 */
	public interface OnPageIndexSelected {
		/**
		 * 结束拖动时，选择页卡
		 * 
		 * @param which
		 */
		public void onSelected(int which);
	}
}
