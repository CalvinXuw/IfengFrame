package com.ifeng.util.ui;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

import com.ifeng.util.Utility;
import com.ifeng.util.motion.SingleTouchDetector;
import com.ifeng.util.ui.IIFAnimation.IFAnimation;

/**
 * 实现android微信客户端对联系人及会话的编辑效果，或者ios7之中滑动编辑状态的效果实现。
 * 
 * @author Calvin
 * 
 */
public class SlideEditLayout extends LinearLayout {

	/** 判断滑动事件的最小位移距离 */
	private static final int MIN_SLIDE_DISTANCE = 20;
	/** 滑动位移缩放比例 */
	private static final float SLIDE_RATIO = 1.5f;

	/** 持有滑动展开View的弱引用实例，用于自动进行收缩操作 */
	private static WeakReference<SlideEditLayout> sWeakReferenceView;
	/** 当前正在进行操作的滑动View */
	private static SlideEditLayout sSlideTouchDetectingInstance;

	/** 事件捕捉器 */
	private SlideTouchDetector mSlideTouchDetector;

	/** 内容View */
	private View mContentView;
	/** 编辑状态的View */
	private View mEditView;

	/**
	 * 构造
	 * 
	 * @param context
	 */
	public SlideEditLayout(Context context) {
		super(context);
		init();
	}

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public SlideEditLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * 设置常态显示内容View
	 * 
	 * @param contentView
	 */
	public void setContentView(View contentView) {
		mContentView = contentView;
		addSlideEditView();
	}

	/**
	 * 设置滑动展开后的编辑View
	 * 
	 * @param editView
	 */
	public void setEditView(View editView) {
		mEditView = editView;
		addSlideEditView();
	}

	/**
	 * 配置滑动编辑View
	 */
	private void addSlideEditView() {
		if (mContentView != null && mEditView != null) {
			removeAllViews();
			setOrientation(LinearLayout.HORIZONTAL);
			addView(mContentView);
			addView(mEditView);
		}
	}

	/**
	 * 初始化
	 */
	private void init() {
		mSlideTouchDetector = new SlideTouchDetector();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// 仅对单一View进行滑动操作
		if (sSlideTouchDetectingInstance != null
				&& sSlideTouchDetectingInstance != this) {
			return super.dispatchTouchEvent(ev);
		}

		mSlideTouchDetector.addMotion(ev);

		// 若判断为横向滑动事件时，进行触摸事件的分发处理
		if (Math.abs(mSlideTouchDetector.getMovingDistanceX()) > MIN_SLIDE_DISTANCE
				* Utility.getDensity(getContext())
				&& Math.abs(mSlideTouchDetector.getMovingDistanceY()) < MIN_SLIDE_DISTANCE
						* Utility.getDensity(getContext())
				|| mSlideTouchDetector.mIsSlideEvent) {

			// 开始记录滑动事件，取消掉子View的事件拦截
			if (!mSlideTouchDetector.mIsSlideEvent) {
				mSlideTouchDetector.startDispatchSlideEvent();
				cancelChildDispatch(this);
			}

			float dx = mContentView.getLeft() - mSlideTouchDetector.getLastDx()
					/ SLIDE_RATIO;
			dx = Math.min(0, Math.max(dx, -mEditView.getMeasuredWidth()));
			layoutSlideEditView(this, (int) dx);

			return true;
		}

		return super.dispatchTouchEvent(ev);
	}

	/**
	 * 收起展开过的View
	 */
	private void closeEditView() {
		// 若当前没有展开的View或者曾经记录的View已经被释放
		if (sWeakReferenceView == null || sWeakReferenceView.get() == null) {
			sWeakReferenceView = null;
			return;
		}

		SlideEditLayout oldView = sWeakReferenceView.get();
		// 若展开的View既是当前正在处理的View
		if (oldView == this) {
			return;
		}

		// 触发收缩动画
		new SlideEditAnimation(oldView, false).startAnimation();
		sWeakReferenceView = null;
	}

	/**
	 * 释放拖动行为，触发展开或收缩动画
	 */
	private void dropEditView() {
		int distance = Math.abs(mContentView.getLeft());

		if (sWeakReferenceView == null || sWeakReferenceView.get() == null) {
			// 试图展开
			if (distance > mEditView.getMeasuredWidth() / 2) {
				new SlideEditAnimation(this, true).startAnimation();
				sWeakReferenceView = new WeakReference<SlideEditLayout>(this);
			} else {
				new SlideEditAnimation(this, false).startAnimation();
			}
		} else {
			// 试图收起
			if (distance < mEditView.getMeasuredWidth() / 2) {
				new SlideEditAnimation(this, false).startAnimation();
				sWeakReferenceView = null;
			} else {
				new SlideEditAnimation(this, true).startAnimation();
			}
		}
	}

	/**
	 * 按照指定的偏移量布局内容View和编辑View
	 * 
	 * @param layout
	 * @param dx
	 */
	private void layoutSlideEditView(SlideEditLayout layout, int dx) {
		layout.mContentView.layout(dx, 0, dx + layout.getMeasuredWidth(),
				layout.getMeasuredHeight());
		layout.mEditView.layout(
				dx + layout.getMeasuredWidth(),
				0,
				dx + layout.getMeasuredWidth()
						+ layout.mEditView.getMeasuredWidth(),
				layout.getMeasuredHeight());
	}

	/**
	 * 滑动效果过度动画
	 * 
	 * @author Calvin
	 * 
	 */
	private class SlideEditAnimation extends IFAnimation {

		/** 默认的动画时长 */
		private static final int ANIM_DURATION = 200;

		/** 滑动View实例 */
		private SlideEditLayout mSlideView;
		/** 起始偏移量记录 */
		private int mStartLeft;
		/** 展开or收缩 */
		private boolean mIsSlideOut;

		/**
		 * 构造
		 * 
		 * @param slideView
		 * @param isSlideOut
		 */
		public SlideEditAnimation(SlideEditLayout slideView, boolean isSlideOut) {
			super(slideView, ANIM_DURATION);

			mSlideView = slideView;
			mIsSlideOut = isSlideOut;
			mStartLeft = slideView.mContentView.getLeft();
		}

		@Override
		public void applyTransformation(float percent) {
			int dPadding = 0;
			if (mIsSlideOut) {
				dPadding = -mSlideView.mEditView.getMeasuredWidth()
						- mStartLeft;
			} else {
				dPadding = 0 - mStartLeft;
			}

			int dx = (int) (mStartLeft + dPadding * percent);
			layoutSlideEditView(mSlideView, dx);
		}

		@Override
		public void onAnimationFinished() {

		}

	}

	/**
	 * 配合{@link #requestDisallowInterceptTouchEvent(boolean)}方法屏蔽父容器中主动实现的事件分配
	 * 
	 * @param parent
	 * @param enable
	 */
	private void setParentSelfDispatchEnable(ViewParent parent, boolean enable) {
		if (parent == null) {
			return;
		}

		parent.requestDisallowInterceptTouchEvent(!enable);
		if (parent instanceof ISelfDispatchEventDelegate) {
			((ISelfDispatchEventDelegate) parent).setSelfDispatchEnable(enable);
		}
		setParentSelfDispatchEnable(parent.getParent(), enable);
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
		} else {
			MotionEvent cancelEvent = MotionEvent.obtain(0, 0,
					MotionEvent.ACTION_CANCEL, 0, 0, 0);
			view.dispatchTouchEvent(cancelEvent);
			cancelEvent.recycle();
		}
	}

	/**
	 * 滑动事件捕捉器
	 * 
	 * @author Calvin
	 * 
	 */
	private class SlideTouchDetector extends SingleTouchDetector {

		/** 是否被定义为滑动事件 */
		private boolean mIsSlideEvent;

		/**
		 * 开始记录滑动事件
		 */
		public void startDispatchSlideEvent() {
			sSlideTouchDetectingInstance = SlideEditLayout.this;
			mIsSlideEvent = true;
			setParentSelfDispatchEnable(SlideEditLayout.this, false);
		}

		@Override
		public void startDispatch(MotionEvent ev) {
			super.startDispatch(ev);
			closeEditView();
		}

		@Override
		public void abandonDispatch() {
			super.abandonDispatch();

			if (mIsSlideEvent) {
				sSlideTouchDetectingInstance = null;
				mIsSlideEvent = false;
				dropEditView();
				setParentSelfDispatchEnable(SlideEditLayout.this, true);
				requestDisallowInterceptTouchEvent(false);
			}
		}

	}
}
