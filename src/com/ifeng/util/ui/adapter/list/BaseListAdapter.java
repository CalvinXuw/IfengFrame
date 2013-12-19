package com.ifeng.util.ui.adapter.list;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

/**
 * 通用listview adapter，包含屏幕适配功能
 * 
 * @author Calvin
 * 
 */
public abstract class BaseListAdapter extends BaseAdapter {

	/** activity */
	private Activity mActivity;

	/**
	 * 构造
	 * 
	 * @param activity
	 */
	public BaseListAdapter(Activity activity) {
		mActivity = activity;
	}

	@Override
	public final View getView(int position, View convertView, ViewGroup parent) {
		View originView = null;
		if (convertView == null) {
			convertView = new FrameLayout(mActivity);
			originView = getView(position, originView);
			((ViewGroup) convertView).addView(originView);
		} else {
			originView = ((ViewGroup) convertView).getChildAt(0);
			originView = getView(position, originView);
		}

		originView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				getViewHeight(position)));

		return convertView;
	}

	/**
	 * 获取需要重新计算高度的原始item view
	 * 
	 * @param position
	 * @param convertView
	 * @return
	 */
	protected abstract View getView(int position, View convertView);

	/**
	 * 获取指定item高度
	 * 
	 * @param position
	 * @return
	 */
	protected int getViewHeight(int position) {
		return LayoutParams.WRAP_CONTENT;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	/**
	 * 获取activity
	 * 
	 * @return
	 */
	protected final Activity getActivity() {
		return mActivity;
	}
}
