package com.ifeng.util.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ifeng.android.R;
import com.ifeng.util.ui.IIFAnimation.IFAnimation;

/**
 * 开关控件
 * 
 * @author Calvin
 * 
 */
public class SwitchButton extends RelativeLayout {

	/** 开关开启文字提示 */
	private static final String SWITCH_ON_TEXT = "ON";
	/** 开关关闭文字提示 */
	private static final String SWITCH_OFF_TEXT = "OFF";

	/** 开关切换监听事件 */
	private OnSwitcherSwitchListener mOnSwitcherSwitchListener;
	/** 是否开启 */
	private boolean mIsOn;
	/** 开关滑块 */
	private TextView mSwitcher;
	/** 是否进行过初始化 */
	private boolean mIsInit;

	/** 滑块宽度 */
	private int mSwitcherWidth;
	/** 滑块高度 */
	private int mSwitcherHeight;

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public SwitchButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if (changed) {
			super.onLayout(changed, left, top, right, bottom);
		}
		init();
	}

	/**
	 * 初始化配置
	 */
	private void init() {
		if (mIsInit) {
			return;
		}

		mSwitcherWidth = (int) (getMeasuredWidth() * 0.6);
		mSwitcherHeight = getMeasuredHeight();

		mSwitcher = new TextView(getContext());
		mSwitcher.setBackgroundColor(getResources().getColor(
				R.color.setting_switch_switcher));
		mSwitcher.setTextColor(getResources().getColor(
				R.color.setting_switch_switcher_text));
		mSwitcher.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
		mSwitcher.setGravity(Gravity.CENTER);
		mSwitcher.setLayoutParams(new RelativeLayout.LayoutParams(
				mSwitcherWidth, mSwitcherHeight));

		addView(mSwitcher);

		SwitcherEventListener switcherEventListener = new SwitcherEventListener();
		setOnTouchListener(switcherEventListener);
		setOnClickListener(switcherEventListener);

		mIsInit = true;
		setSwitchInternal(mIsOn, false);
	}

	/**
	 * 设置当前开关状态
	 * 
	 * @param isOn
	 */
	public void setSwitch(boolean isOn) {
		setSwitchInternal(isOn, false);
	}

	/**
	 * 内部控制滑块状态
	 * 
	 * @param isOn
	 * @param isAnim
	 */
	private void setSwitchInternal(boolean isOn, boolean isAnim) {
		mIsOn = isOn;
		if (!mIsInit && mOnSwitcherSwitchListener != null) {
			return;
		}
		updateSwitcherState();
		new MovingAnimation(isAnim).startAnimation();
	}

	/**
	 * 更新滑块显示状态
	 */
	private void updateSwitcherState() {
		if (mIsOn) {
			mSwitcher.setText(SWITCH_ON_TEXT);
		} else {
			mSwitcher.setText(SWITCH_OFF_TEXT);
		}
	}

	/**
	 * 加入监听回调
	 * 
	 * @param onSwitcherSwitchListener
	 */
	public void setOnSwitcherSwitchListener(
			OnSwitcherSwitchListener onSwitcherSwitchListener) {
		this.mOnSwitcherSwitchListener = onSwitcherSwitchListener;
	}

	/**
	 * 开关切换事件回调
	 * 
	 * @author Calvin
	 * 
	 */
	public interface OnSwitcherSwitchListener {
		/**
		 * 开关切换
		 * 
		 * @param isOn
		 *            是否为开启状态
		 */
		public void onSwitch(boolean isOn);
	}

	/*
	 * 补间动画部分
	 */
	private class MovingAnimation extends IFAnimation {

		/** 动画时间 */
		private static final int ANIMATION_DURATION = 200;

		/** 起始位置X坐标 */
		private int mStartX;
		/** 位移差 */
		private int mDistanceX;

		/**
		 * 构造
		 * 
		 * @param isAnim
		 *            是否需要动画
		 */
		public MovingAnimation(boolean isAnim) {
			super(SwitchButton.this, isAnim ? ANIMATION_DURATION : 0);
			setInterpolator(new DecelerateInterpolator());

			mStartX = mSwitcher.getLeft();
			if (mIsOn) {
				mDistanceX = getWidth() - mSwitcherWidth - mStartX;
			} else {
				mDistanceX = 0 - mStartX;
			}
		}

		@Override
		public void applyTransformation(float percent) {
			int newLeft = (int) (mStartX + mDistanceX * percent);
			mSwitcher.layout(newLeft, 0, newLeft + mSwitcherWidth,
					mSwitcherHeight);
		}

		@Override
		public void onAnimationFinished() {

		}
	}

	/*
	 * 滑块拖拽/点击部分
	 */
	private class SwitcherEventListener extends OnSingleClickListener implements
			OnTouchListener {

		/** 是否已经被滑动事件拦截 */
		private boolean mIsDispatchByDrag;
		/** 上一次触摸事件的X坐标 */
		private float mLastX;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mLastX = event.getX();

				mIsDispatchByDrag = false;
				break;
			case MotionEvent.ACTION_MOVE:
				mIsDispatchByDrag = true;
				float dx = event.getX() - mLastX;
				mLastX = event.getX();

				int newLeft = (int) (mSwitcher.getLeft() + dx);
				newLeft = Math.max(0,
						Math.min(getWidth() - mSwitcherWidth, newLeft));

				mSwitcher.layout(newLeft, 0, newLeft + mSwitcherWidth,
						mSwitcherHeight);
				if (mSwitcher.getLeft() > (getWidth() - mSwitcherWidth) / 2) {
					mIsOn = true;
				} else {
					mIsOn = false;
				}
				updateSwitcherState();

				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (mIsDispatchByDrag) {
					setSwitchInternal(mIsOn, true);
					mOnSwitcherSwitchListener.onSwitch(mIsOn);
				}
				break;
			}
			return false;
		}

		@Override
		public void onSingleClick(View v) {
			if (!mIsDispatchByDrag) {
				setSwitchInternal(!mIsOn, true);
				mOnSwitcherSwitchListener.onSwitch(mIsOn);
			}
		}

	}
}
