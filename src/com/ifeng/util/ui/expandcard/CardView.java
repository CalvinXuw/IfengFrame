package com.ifeng.util.ui.expandcard;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ifeng.android.R;
import com.ifeng.util.ui.OnSingleClickListener;

public class CardView extends RelativeLayout {

	/** card view id 起始位置 */
	private static int CARD_VIEW_ID = 19890518;

	/** 展开高度 */
	private static int sCardExpandHeight;
	/** 收起高度 */
	private static int sCardShrinkHeight;

	/** 每个CardView持有一张Card */
	private Card mCard;
	/** 在内部包裹Card的容器，不受Card外部的height制约 */
	private FrameLayout mInnerLayout;
	/** 提供Fragment更迭Fragment的viewId */
	private int mCardId;
	/** 退出详情按钮 */
	private Button mExitBtn;
	/** 半透明遮罩层 */
	private View mAlphaView;
	/** 标题 */
	private TextView mTitleTx;
	/** 上阴影 */
	private View mShadowTop;
	/** 下阴影 */
	private View mShadowBottom;

	/** 对于卡片动作的回调 */
	private CardViewCallback mCallback;
	/** fragment manager */
	private FragmentManager mFragmentManager;

	/** 透明度 */
	private float mAlpha;

	/**
	 * 构造
	 * 
	 * @param context
	 * @param card
	 */
	public CardView(Context context, Card card) {
		super(context);
		mCard = card;
	}

	/**
	 * 构造
	 * 
	 * @param context
	 */
	private CardView(Context context) {
		super(context);
	}

	/**
	 * 对于卡片的初始化
	 * 
	 * @param callback
	 * @param fragmentManager
	 */
	protected void initWithExpandView(CardViewCallback callback,
			FragmentManager fragmentManager) {
		// 适配换算，原设计分辨率为1280*720，卡片为94.5/350dip高度,density=2
		DisplayMetrics dm = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay()
				.getMetrics(dm);
		float scale = (dm.widthPixels / dm.density) / (720 / 2);
		if (scale < 1) {
			scale = 1;
		}
		sCardShrinkHeight = (int) (mCard.mCardShrinkHeight * scale * dm.density);
		sCardExpandHeight = (int) (mCard.mCardExpandHeight * scale * dm.density);

		mCallback = callback;
		mFragmentManager = fragmentManager;

		/*
		 * 加入Fragment的容器layout
		 */
		mInnerLayout = new FrameLayout(getContext());
		mInnerLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				sCardExpandHeight));
		addView(mInnerLayout);

		mInnerLayout.setId(CARD_VIEW_ID);
		mCardId = CARD_VIEW_ID;

		CARD_VIEW_ID++;

		mCard.initWithCardcallback(mCallback);

		/*
		 * 加入返回键
		 */
		mExitBtn = new Button(getContext());
		mExitBtn.setBackgroundResource(R.drawable.btn_index_back);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				callback.dip2pxInView(100), callback.dip2pxInView(75));
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		mExitBtn.setLayoutParams(params);
		mExitBtn.setVisibility(View.GONE);
		mExitBtn.setOnClickListener(new OnSingleClickListener() {
			@Override
			public void onSingleClick(View v) {
				mCallback.onCardExit(CardView.this);
			}
		});
		addView(mExitBtn);

		/*
		 * 加入标题
		 */
		LinearLayout titleContainer = new LinearLayout(getContext());
		titleContainer.setGravity(Gravity.CENTER);
		titleContainer.setBackgroundResource(R.drawable.background_index_title);
		mTitleTx = new TextView(getContext());
		mTitleTx.setText(mCard.mTitle);
		RelativeLayout.LayoutParams titleparams = new RelativeLayout.LayoutParams(
				callback.dip2pxInView(85), callback.dip2pxInView(26));
		titleparams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		titleparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		titleparams.setMargins(0, 0, callback.dip2pxInView(10), 0);
		titleContainer.setLayoutParams(titleparams);
		mTitleTx.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
		mTitleTx.setGravity(Gravity.CENTER);
		mTitleTx.setPadding(callback.dip2pxInView(5), 0, 0, 0);
		View imageTag = new View(getContext());
		imageTag.setLayoutParams(new LinearLayout.LayoutParams(callback
				.dip2pxInView(6), callback.dip2pxInView(6)));
		imageTag.setBackgroundResource(R.drawable.image_index_title_tag);

		titleContainer.addView(imageTag);
		titleContainer.addView(mTitleTx);
		addView(titleContainer);

		/*
		 * 半透明遮罩
		 */
		mAlphaView = new View(getContext());
		mAlphaView.setBackgroundColor(Color.BLACK);
		setCardViewAlpha(1);
		mAlphaView.setOnClickListener(mCardClickListener);
		addView(mAlphaView);

		/*
		 * 加入阴影线
		 */
		mShadowTop = new View(getContext());
		RelativeLayout.LayoutParams topshadowparams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.FILL_PARENT,
				callback.dip2pxInView(10));
		topshadowparams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mShadowTop.setLayoutParams(topshadowparams);
		mShadowTop.setBackgroundResource(R.drawable.shadow_card_down);
		addView(mShadowTop);
		mShadowBottom = new View(getContext());
		RelativeLayout.LayoutParams bottomshadowparams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.FILL_PARENT,
				callback.dip2pxInView(10));
		bottomshadowparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		mShadowBottom.setLayoutParams(bottomshadowparams);
		mShadowBottom.setBackgroundResource(R.drawable.shadow_card_up);
		addView(mShadowBottom);
		mShadowTop.getBackground().setAlpha((int) (255 * 0.8f));
		mShadowBottom.getBackground().setAlpha((int) (255 * 0.8f));

		if (!mCard.mNeedShadow) {
			removeView(mShadowTop);
			removeView(mShadowBottom);
		}

		setState(CardState.SHRINK);
	}

	/**
	 * 卡片点击回调
	 */
	private OnClickListener mCardClickListener = new OnSingleClickListener() {

		@Override
		public void onSingleClick(View v) {
			ViewParent vp = v.getParent();
			if (vp instanceof CardView) {
				mCallback.onCardClick((CardView) vp);
			}
		}
	};

	/**
	 * 根据状态改变卡片视图
	 * 
	 * @param state
	 */
	protected void setState(CardState state) {
		if (state == CardState.EXPAND) {
			if (mFragmentManager.findFragmentById(mCardId) != mCard.mDetailFragment) {
				mFragmentManager
						.beginTransaction()
						.replace(mCardId, mCard.mDetailFragment)
						.setTransition(
								FragmentTransaction.TRANSIT_FRAGMENT_FADE)
						.commit();
			}
			mExitBtn.setVisibility(View.VISIBLE);
			mTitleTx.setTextColor(Color.WHITE);
			mAlphaView.setClickable(false);

			mShadowTop.setVisibility(View.GONE);
			mShadowBottom.setVisibility(View.VISIBLE);
		} else if (state == CardState.NOT_THE_EXPAND) {
			mExitBtn.setVisibility(View.GONE);
			mTitleTx.setTextColor(Color.WHITE);
			mAlphaView.setClickable(true);

			mShadowTop.setVisibility(View.GONE);
			mShadowBottom.setVisibility(View.VISIBLE);

			/*
			 * 当前设置为仅显示一页展开效果，其余均为cover页面
			 */
			if (mFragmentManager.findFragmentById(mCardId) != mCard.mCoverFragment) {
				mFragmentManager
						.beginTransaction()
						.replace(mCardId, mCard.mCoverFragment)
						.setTransition(
								FragmentTransaction.TRANSIT_FRAGMENT_FADE)
						.commit();
			}
		} else if (state == CardState.AFTER_EXPAND) {
			mExitBtn.setVisibility(View.GONE);
			mTitleTx.setTextColor(Color.WHITE);
			mAlphaView.setClickable(true);

			mShadowTop.setVisibility(View.GONE);
			mShadowBottom.setVisibility(View.VISIBLE);

			/*
			 * 当前设置为仅显示一页展开效果，其余均为cover页面
			 */
			if (mFragmentManager.findFragmentById(mCardId) != mCard.mCoverFragment) {
				mFragmentManager
						.beginTransaction()
						.replace(mCardId, mCard.mCoverFragment)
						.setTransition(
								FragmentTransaction.TRANSIT_FRAGMENT_FADE)
						.commit();
			}
		} else if (state == CardState.SHRINK) {
			if (mFragmentManager.findFragmentById(mCardId) != mCard.mCoverFragment) {
				mFragmentManager
						.beginTransaction()
						.replace(mCardId, mCard.mCoverFragment)
						.setTransition(
								FragmentTransaction.TRANSIT_FRAGMENT_FADE)
						.commit();
			}
			mExitBtn.setVisibility(View.GONE);
			mTitleTx.setTextColor(Color.WHITE);
			mAlphaView.setClickable(true);
			mShadowTop.setVisibility(View.GONE);
			mShadowBottom.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * 设置卡片遮罩透明度
	 * 
	 * @param percent
	 */
	protected void setCardViewAlpha(float percent) {
		mAlpha = Math.max(Math.min(1f, percent), 0);
		mAlphaView.getBackground().setAlpha((int) (255 * (1 - mAlpha)));
	}

	/**
	 * 获取卡片遮罩透明度
	 * 
	 * @return
	 */
	protected float getCardViewAlpha() {
		return Math.max(Math.min(1f, mAlpha), 0);
	}

	/**
	 * 获取换算过的卡片展开后的高度
	 * 
	 * @return
	 */
	protected int getCardExpandHeight() {
		return sCardExpandHeight;
	}

	/**
	 * 获取换算过的卡片收起后的高度
	 * 
	 * @return
	 */
	protected int getCardShrinkHeight() {
		return sCardShrinkHeight;
	}

	/**
	 * 用来加入到ExpandView的卡片，包含了用以展开的详情页面以及该卡片的标题、封面资源id
	 * 
	 * @author Calvin
	 * 
	 */
	public static class Card {

		/** 卡片展开高度，dip单位 */
		private float mCardExpandHeight = 350;
		/** 卡片收起高度，dip单位 */
		private float mCardShrinkHeight = 94.5f;
		/** 是否需要上下阴影层 */
		private boolean mNeedShadow = true;

		/** 卡片名 */
		private String mTitle;
		/** 在收起状态下的封面资源 */
		private int mCoverDrawableRes;
		/** 展开状态的详情页 */
		private DetailFragment mDetailFragment;
		/** 封面页 */
		private ImageFragment mCoverFragment;

		/**
		 * 构造
		 * 
		 * @param title
		 * @param detailFragment
		 * @param coverDrawableRes
		 */
		public Card(String title, DetailFragment detailFragment,
				int coverDrawableRes) {
			mTitle = title;
			mCoverDrawableRes = coverDrawableRes;
			mDetailFragment = detailFragment;
			mCoverFragment = new ImageFragment();
		}

		/**
		 * 构造
		 * 
		 * @param title
		 * @param detailFragment
		 * @param coverDrawableRes
		 */
		public Card(String title, DetailFragment detailFragment,
				int coverDrawableRes, float expandHeight, float shrinkHeight,
				boolean needShadow) {
			mTitle = title;
			mCardExpandHeight = expandHeight;
			mCardShrinkHeight = shrinkHeight;
			mCoverDrawableRes = coverDrawableRes;
			mDetailFragment = detailFragment;
			mNeedShadow = needShadow;
			mCoverFragment = new ImageFragment();
		}

		/**
		 * 不对外暴露的初始化方法，将封面资源id构造成封面页
		 * 
		 * @param callback
		 */
		private void initWithCardcallback(CardViewCallback callback) {
			Bundle args = new Bundle();
			args.putInt(ImageFragment.KEY_IMAGE_RES_ID, mCoverDrawableRes);
			mCoverFragment.setArguments(args);
		}
	}

	/**
	 * card fragment基类
	 * 
	 * @author Calvin
	 * 
	 */
	private abstract static class CardFragment extends Fragment {

		/** 像素密度 */
		protected float mDensity;

		/**
		 * 构造
		 */
		public CardFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			DisplayMetrics dm = new DisplayMetrics();
			getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
			mDensity = dm.density;
		}

		/**
		 * 提供对当前分辨率下dip px的换算
		 * 
		 * @param dip
		 * @return
		 */
		protected int dip2px(int dip) {
			return (int) (dip * mDensity);
		}
	}

	/**
	 * 封面页，可扩展使其对网络资源图片进行支持
	 * 
	 * @author Calvin
	 * 
	 */
	public static class ImageFragment extends CardFragment {

		/** res id key */
		private static final String KEY_IMAGE_RES_ID = "image_res_id";
		/** res id */
		private int mImageResId;

		/**
		 * must have a empty fragment，必须有一个空的构造方法，系统会时不时的调一下
		 */
		public ImageFragment() {

		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			mImageResId = getArguments().getInt(KEY_IMAGE_RES_ID);

		}

		@Override
		public View onCreateView(LayoutInflater inflater,
				final ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.card_fragment_image,
					container, false);
			ImageView image = (ImageView) view.findViewById(R.id.image_card);
			/*
			 * 可引入ImageFetcher进行网络图片加载
			 */
			image.setImageBitmap(BitmapFactory.decodeStream(getResources()
					.openRawResource(mImageResId)));
			image.setScaleType(ScaleType.FIT_XY);
			view.setBackgroundColor(Color.rgb(68, 68, 68));
			image.setLayoutParams(new RelativeLayout.LayoutParams(
					LayoutParams.FILL_PARENT, sCardShrinkHeight));
			view.setLayoutParams(new ViewGroup.LayoutParams(
					LayoutParams.FILL_PARENT, sCardExpandHeight));

			return view;
		}
	}

	/**
	 * 详情内容页的Fragment，在Card中子类需要继承自该类，并且从该类的onCreateView中获取到ViewContainer
	 * 
	 * @author Calvin
	 * 
	 */
	public static abstract class DetailFragment extends CardFragment {

		/**
		 * must have a empty fragment，必须有一个空的构造方法，系统会时不时的调一下
		 */
		public DetailFragment() {
		}

		@Override
		public final View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			/*
			 * 子类调用后，父类设定卡片高度
			 */
			View child = createViewForParent(inflater, container,
					savedInstanceState);
			FrameLayout parent = new FrameLayout(getActivity());
			parent.setLayoutParams(new android.view.ViewGroup.LayoutParams(
					LayoutParams.FILL_PARENT, sCardExpandHeight));
			parent.addView(child);

			return parent;
		}

		/**
		 * 子类提供给父类需要展示的卡片
		 * 
		 * @param inflater
		 * @param container
		 * @param savedInstanceState
		 * @return
		 */
		protected abstract View createViewForParent(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState);
	}

	/**
	 * 对CardView的行为支撑
	 * 
	 * @author Calvin
	 * 
	 */
	protected interface CardViewCallback extends Parcelable {

		/**
		 * 当封面被点击时
		 * 
		 * @param cardView
		 */
		public void onCardClick(CardView cardView);

		/**
		 * 当退出详情页面时
		 * 
		 * @param cardView
		 */
		public void onCardExit(CardView cardView);

		/**
		 * 提供当前分辨率下对dip px之间的转换
		 * 
		 * @param dip
		 * @return
		 */
		public int dip2pxInView(int dip);
	}

	/**
	 * 卡片状态，当前展开、展开之外的、展开的后一个、收起状态
	 * 
	 * @author Calvin
	 * 
	 */
	protected enum CardState {
		EXPAND, NOT_THE_EXPAND, AFTER_EXPAND, SHRINK
	}
}