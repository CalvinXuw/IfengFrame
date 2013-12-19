package com.ifeng.util.ui.adapter.list;

import android.app.Activity;
import android.view.View;
import android.widget.AbsListView.LayoutParams;
import android.widget.LinearLayout;

/**
 * 组合形式的列表adapter，适用于列表存在多种样式混合的情况。
 * 
 * @author Calvin
 * 
 */
public abstract class BaseComposeListAdapter extends BaseListAdapter {

	/**
	 * 构造
	 * 
	 * @param activity
	 */
	public BaseComposeListAdapter(Activity activity) {
		super(activity);
	}

	/**
	 * 获取包含的样式数量
	 * 
	 * @return
	 */
	protected abstract int getComposeStyleCount();

	/**
	 * 根据样式获取不填充数据的子项模板View
	 * 
	 * @param style
	 * @return
	 */
	protected abstract View getComposeItemMouldView(int style);

	/**
	 * 根据样式获取子项View
	 * 
	 * @param converView
	 * @param style
	 * @param position
	 * @return
	 */
	protected abstract View getComposeItemView(View converView, int style,
			int position);

	/**
	 * 获取指定位置的视图样式
	 * 
	 * @param position
	 * @return
	 */
	protected abstract int getItemComposeStyle(int position);

	@Override
	protected final View getView(int position, View convertView) {
		LinearLayout container = null;
		if (convertView == null) {
			container = new LinearLayout(getActivity());
			container.setOrientation(LinearLayout.VERTICAL);
			LayoutParams layoutParams = new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			container.setLayoutParams(layoutParams);

			for (int i = 0; i < getComposeStyleCount(); i++) {
				View subItemView = getComposeItemMouldView(i);
				subItemView.setId(i);
				container.addView(subItemView);
			}
		} else {
			container = (LinearLayout) convertView;
		}

		int currentStyle = getItemComposeStyle(position);
		for (int i = 0; i < container.getChildCount(); i++) {
			View subItemView = container.getChildAt(i);
			if (subItemView.getId() != currentStyle) {
				subItemView.setVisibility(View.GONE);
			} else {
				subItemView.setVisibility(View.VISIBLE);
				getComposeItemView(subItemView, currentStyle, position);
			}
		}

		return container;
	}
}
