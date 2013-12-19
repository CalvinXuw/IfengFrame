package com.ifeng.util.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ifeng.android.R;

/**
 * 顶部导航条View，如仅需返回功能，则直接调用{@link #setBackItem(Activity)}
 * 
 * @author Calvin
 * 
 */
public class NavgationbarView extends RelativeLayout {

	/**
	 * 导航条style
	 * 
	 * @author Calvin
	 * 
	 */
	public enum Style {
		LIGHT, DARK
	}

	/** 左边item */
	private FrameLayout mLeftItem;
	/** 中间item */
	private FrameLayout mMiddleItem;
	/** 右边item */
	private FrameLayout mRightItem;
	/** title */
	private TextView mTitle;
	/** divider */
	private View mDivider;

	/** 是否已经进行了初始化 */
	private boolean mIsInit;
	/** 当前导航条设定style */
	private Style mStyle = Style.LIGHT;

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public NavgationbarView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * 初始化视图
	 */
	private void init() {
		if (mIsInit) {
			return;
		}

		mIsInit = true;

		mLeftItem = (FrameLayout) findViewById(R.id.layout_nav_item_left);
		mMiddleItem = (FrameLayout) findViewById(R.id.layout_nav_item_middle);
		mRightItem = (FrameLayout) findViewById(R.id.layout_nav_item_right);
		mTitle = (TextView) findViewById(R.id.text_title);
		mDivider = findViewById(R.id.divider_navigation);

		if (mLeftItem == null || mRightItem == null || mTitle == null) {
			throw new IllegalArgumentException(
					"Navgationbar need to use from include layout ! ");
		}

		DisplayMetrics dm = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay()
				.getMetrics(dm);
		FrameLayout.LayoutParams oldParams = (FrameLayout.LayoutParams) mTitle
				.getLayoutParams();
		oldParams.width = dm.widthPixels * 2 / 3;
		mTitle.setLayoutParams(oldParams);

		setStyle(mStyle);
	}

	/**
	 * 设置导航条类型
	 * 
	 * @param style
	 */
	public void setStyle(Style style) {
		init();
		this.mStyle = style;
		if (mStyle == Style.LIGHT) {
			mTitle.setTextColor(getContext().getResources().getColor(
					R.color.font_title));
			setBackgroundColor(getContext().getResources().getColor(
					R.color.background_navigation));
			mDivider.setBackgroundColor(getContext().getResources().getColor(
					R.color.background_navigation_divider));
		} else if (mStyle == Style.DARK) {
			mTitle.setTextColor(getContext().getResources().getColor(
					R.color.font_list_cover));
			setBackgroundColor(getContext().getResources().getColor(
					R.color.background_navigation_dark));
			mDivider.setBackgroundColor(getContext().getResources().getColor(
					R.color.background_navigation_divider_dark));
		}
	}

	/**
	 * 设置标题
	 * 
	 * @param text
	 */
	public void setTitle(String text) {
		init();
		mTitle.setText(text);
	}

	/**
	 * 设置左边item
	 * 
	 * @param item
	 */
	public void setLeftItem(View item) {
		init();
		mLeftItem.removeAllViews();
		mLeftItem.addView(item);
	}

	/**
	 * 设置中间item
	 * 
	 * @param item
	 */
	public void setMiddleItem(View item) {
		init();
		mMiddleItem.removeAllViews();
		mMiddleItem.addView(item);
	}

	/**
	 * 设置右边item
	 * 
	 * @param item
	 */
	public void setRightItem(View item) {
		init();
		mRightItem.removeAllViews();
		mRightItem.addView(item);
	}

	/**
	 * 添加自定义视图返回键
	 * 
	 * @param activity
	 * @param itemView
	 */
	public void setBackItem(final Activity activity, View itemView) {
		itemView.setOnClickListener(new OnSingleClickListener() {

			@Override
			public void onSingleClick(View v) {
				activity.finish();
			}
		});
		setLeftItem(itemView);
	}

	/**
	 * 添加返回键，会替换掉左item
	 * 
	 * @param activity
	 */
	public void setBackItem(Activity activity) {
		int res = mStyle == Style.LIGHT ? R.layout.nav_back
				: R.layout.nav_back_dark;
		setBackItem(activity, activity.getLayoutInflater().inflate(res, null));
	}
}
