package com.ifeng.util.ui.expandcard;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.ifeng.util.ui.IIFAnimation.IFAnimation;
import com.ifeng.util.ui.expandcard.CardView.Card;
import com.ifeng.util.ui.expandcard.CardView.CardState;
import com.ifeng.util.ui.expandcard.CardView.CardViewCallback;

public class ExpandCardView extends FrameLayout implements CardViewCallback {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4986314175372892753L;
	/** 像素密度 */
	private static float mDensity;
	/** 屏幕宽度 */
	private static int mWidth;
	/** FragmentManager */
	private FragmentManager mFragmentManager;

	/** 卡片容器 */
	private ScrollView mContainerScrollView;
	private LinearLayout mContainerLinearLayout;
	private List<CardView> mCardLayouts;

	private CardView mCurrentCardView;

	public ExpandCardView(Context context) {
		super(context);
		init();
	}

	public ExpandCardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ExpandCardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
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

		mFragmentManager = ((FragmentActivity) getContext())
				.getSupportFragmentManager();

		mContainerScrollView = new NoScrollView(getContext());
		mContainerLinearLayout = new LinearLayout(getContext());

		LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);
		mContainerScrollView.setLayoutParams(layoutParams);
		mContainerLinearLayout.setLayoutParams(layoutParams);

		mContainerScrollView.addView(mContainerLinearLayout);
		addView(mContainerScrollView);

		mContainerScrollView.setVerticalScrollBarEnabled(false);
		mContainerLinearLayout.setOrientation(LinearLayout.VERTICAL);

	}

	/**
	 * 添加卡片布局
	 * 
	 * @param cards
	 */
	public void addCards(Card... cards) {
		if (cards == null || cards.length == 0) {
			throw new IllegalArgumentException("cards can not be empty");
		}

		mCardLayouts = new LinkedList<CardView>();
		mContainerLinearLayout.removeAllViews();

		for (Card card : cards) {
			/*
			 * 创建单个卡片。并参照CardView中的设定进行编排
			 */
			CardView cardView = new CardView(getContext(), card);
			cardView.initWithExpandView(this, mFragmentManager);
			// 根据原尺寸720*200进行适配调整
			android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(
					android.widget.LinearLayout.LayoutParams.FILL_PARENT,
					cardView.getCardShrinkHeight());
			cardView.setLayoutParams(layoutParams);
			mContainerLinearLayout.addView(cardView);

			mCardLayouts.add(cardView);
		}
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
	private int px2dip(int px) {
		return (int) (px / mDensity);
	}

	/** 卡片补间动画 */
	private MovingAnimation mMovingAnimation;

	/**
	 * 卡片补间动画
	 * 
	 * @author Calvin
	 * 
	 */
	private class MovingAnimation extends IFAnimation {

		/** 持续时间 */
		private static final int DURATION = 300;

		/** alpha变化 */
		private final float D_ALPHA = 0.7f;

		/** 等待展开的卡片 */
		private CardView mToExpand;
		/** 等待收起的卡片 */
		private CardView mToShrink;

		/** 展开高度 */
		private int mExpandHeight;
		/** 收起高度 */
		private int mShrinkHeight;

		/** 原始滚动条位置 */
		private int mOriginScrollerScroll;
		/** 目标滚动条位置 */
		private int mTargetScrollerScroll;

		/** 显现的alpha */
		private float mAlphaToShow;
		/** 淡化的alpha */
		private float mAlphaToHide;

		/**
		 * 构造
		 * 
		 * @param toExpand
		 * @param toShrink
		 */
		public MovingAnimation(CardView toExpand, CardView toShrink) {
			super(ExpandCardView.this, DURATION);

			mToExpand = toExpand;
			mToShrink = toShrink;

			CardView cardView = toExpand != null ? toExpand : toShrink;
			mExpandHeight = cardView.getCardExpandHeight();
			mShrinkHeight = cardView.getCardShrinkHeight();

			mOriginScrollerScroll = mContainerScrollView.getScrollY();
			mTargetScrollerScroll = mContainerScrollView.getScrollY();

			/*
			 * 计算从初始到展开或切换展开的状态
			 */
			if (mToExpand != null) {
				mTargetScrollerScroll = (mCardLayouts.indexOf(mToExpand)
						* mToExpand.getCardShrinkHeight() - (getMeasuredHeight() - mToExpand
						.getCardExpandHeight()) / 2);
			}

			mAlphaToShow = 1.0f;
			mAlphaToHide = 1 - D_ALPHA;
		}

		@Override
		public void applyTransformation(float percent) {
			ViewGroup.LayoutParams layoutParams = null;
			if (mToExpand != null) {
				layoutParams = mToExpand.getLayoutParams();
				layoutParams.height = (int) (mShrinkHeight + (mExpandHeight - mShrinkHeight)
						* percent);
				mToExpand.setLayoutParams(layoutParams);
			}

			if (mToShrink != null) {
				layoutParams = mToShrink.getLayoutParams();
				layoutParams.height = (int) (mExpandHeight - (mExpandHeight - mShrinkHeight)
						* percent);
				mToShrink.setLayoutParams(layoutParams);
			}

			mContainerScrollView
					.scrollTo(
							0,
							(int) (mOriginScrollerScroll + (mTargetScrollerScroll - mOriginScrollerScroll)
									* percent));

			/*
			 * 从初始到展开
			 */
			if (mToExpand != null && mToShrink == null) {
				for (CardView cardView : mCardLayouts) {
					if (cardView == mToExpand) {
						cardView.setCardViewAlpha(mAlphaToHide
								+ (mAlphaToShow - mAlphaToHide) * percent);
					} else {
						cardView.setCardViewAlpha(mAlphaToShow
								- (mAlphaToShow - mAlphaToHide) * percent);
					}
				}
			}

			/*
			 * 切换展开
			 */
			if (mToExpand != null && mToShrink != null) {
				mToExpand.setCardViewAlpha(mAlphaToHide
						+ (mAlphaToShow - mAlphaToHide) * percent);
				mToShrink.setCardViewAlpha(mAlphaToShow
						- (mAlphaToShow - mAlphaToHide) * percent);
			}

			/*
			 * 收起
			 */
			if (mToExpand == null && mToShrink != null) {
				for (CardView cardView : mCardLayouts) {
					if (cardView.getCardViewAlpha() != mAlphaToShow) {
						cardView.setCardViewAlpha(mAlphaToHide
								+ (mAlphaToShow - mAlphaToHide) * percent);
					}
				}
			}
		}

		@Override
		public void onAnimationFinished() {

		}

	}

	/** 起始Y坐标 */
	private float mStartY;
	/** 上次Y坐标 */
	private float mLastY;
	/** 事件被捕捉 */
	private boolean mDispatched;
	/** 捕捉到位移自动切换卡片后，取消滚动操作 */
	private boolean mCanceledScroll;
	/** 缓慢滑动系数 */
	private final float RATIO = 1.5f;

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		/*
		 * 若正在进行动画，则不做任何响应
		 */
		if (mMovingAnimation != null && !mMovingAnimation.isAnimationEnded()) {
			return true;
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			/*
			 * 记录起始信息
			 */
			mStartY = ev.getY();
			mLastY = ev.getY();
			mDispatched = true;
			mCanceledScroll = false;
			break;
		case MotionEvent.ACTION_MOVE:
			float dy = ev.getY() - mLastY;
			float dySinceStart = (ev.getY() - mStartY) / RATIO;
			mLastY = ev.getY();

			/*
			 * 自动切换卡片事件触发
			 */
			if ((mMovingAnimation == null || mMovingAnimation
					.isAnimationEnded())
					&& mDispatched
					&& mCurrentCardView != null
					&& Math.abs(dySinceStart) > mCurrentCardView
							.getCardShrinkHeight()) {
				int indexNext = -1;
				if (dySinceStart > 0) {
					indexNext = mCardLayouts.indexOf(mCurrentCardView) - 1;
				} else {
					indexNext = mCardLayouts.indexOf(mCurrentCardView) + 1;
				}

				if (indexNext >= 0 && indexNext < mCardLayouts.size()) {
					onCardClick(mCardLayouts.get(indexNext));
					mDispatched = false;
					mCanceledScroll = true;
				}
			}

			/*
			 * 操作滚动条
			 */
			if (!mCanceledScroll) {
				mContainerScrollView.scrollBy(0, -(int) (dy / RATIO));
			}
			break;
		case MotionEvent.ACTION_UP:
			/*
			 * 松开手指，自动回弹效果
			 */
			if ((mMovingAnimation == null || mMovingAnimation
					.isAnimationEnded()) && mCurrentCardView != null) {
				mContainerScrollView.smoothScrollTo(
						0,
						mCardLayouts.indexOf(mCurrentCardView)
								* mCurrentCardView.getCardShrinkHeight()
								- (getMeasuredHeight() - mCurrentCardView
										.getCardExpandHeight()) / 2);
			}
			/*
			 * 恢复初始状态
			 */
			mDispatched = false;
			mCanceledScroll = false;
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	@Override
	public void onCardClick(final CardView cardView) {
		if (mMovingAnimation != null && !mMovingAnimation.isAnimationEnded()) {
			return;
		}

		CardView oldCardView = mCurrentCardView;
		CardView newCardView = cardView;

		if (mCurrentCardView != null) {
			/*
			 * 改变原始卡片状态
			 */
			if (mCardLayouts.indexOf(oldCardView) == mCardLayouts
					.indexOf(newCardView) + 1) {
				oldCardView.setState(CardState.AFTER_EXPAND);
			} else if (oldCardView != newCardView) {
				oldCardView.setState(CardState.NOT_THE_EXPAND);
			}

			mMovingAnimation = new MovingAnimation(newCardView,
					mCurrentCardView);
		} else {
			mMovingAnimation = new MovingAnimation(newCardView, null);
		}

		/*
		 * 改变选中卡片状态
		 */
		newCardView.setState(CardState.EXPAND);
		mCurrentCardView = newCardView;

		/*
		 * 改变剩余卡片状态
		 */
		for (CardView card : mCardLayouts) {
			if (card != newCardView && card != oldCardView) {
				if (mCardLayouts.indexOf(card) == mCardLayouts
						.indexOf(newCardView) + 1) {
					card.setState(CardState.AFTER_EXPAND);
				} else if (card != newCardView) {
					card.setState(CardState.NOT_THE_EXPAND);
				}
			}
		}

		mMovingAnimation.startAnimation();
	}

	@Override
	public void onCardExit(CardView cardView) {
		if (mMovingAnimation != null && !mMovingAnimation.isAnimationEnded()) {
			return;
		}

		mCurrentCardView = null;
		mMovingAnimation = new MovingAnimation(null, cardView);
		mMovingAnimation.startAnimation();

		for (CardView card : mCardLayouts) {
			card.setState(CardState.SHRINK);
		}
	}

	/**
	 * 是否为展开状态
	 * 
	 * @return
	 */
	public boolean isExpand() {
		return mCurrentCardView != null;
	}

	/**
	 * 收起
	 */
	public void shrink() {
		if (isExpand()) {
			onCardExit(mCurrentCardView);
		}
	}

	@Override
	public int dip2pxInView(int dip) {
		return dip2px(dip);
	}

	/**
	 * 禁止滚动的ScrollView，将事件依托于ExpandCardView中
	 * 
	 * @author Calvin
	 * 
	 */
	private class NoScrollView extends ScrollView {
		/**
		 * 构造
		 * 
		 * @param context
		 */
		public NoScrollView(Context context) {
			super(context);
			setVerticalFadingEdgeEnabled(false);
		}

		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			return false;
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
	}
}
