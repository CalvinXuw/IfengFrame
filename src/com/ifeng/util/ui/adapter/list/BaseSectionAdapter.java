package com.ifeng.util.ui.adapter.list;

import android.app.Activity;
import android.view.View;

import com.ifeng.NeedOverrideException;

/**
 * 基于{@link BaseComposeListAdapter}实现的分栏list adapter，缺省为单一样式的adapter，重写
 * {@link #getComposeStyleCount}、 {@link #getItemComposeStyle(int, int)}、
 * {@link #getComposeItemView(View, int, int, int)}方法可获取组合样式的BaseSectionAdapter。
 * 
 * @author Calvin
 * 
 */
public abstract class BaseSectionAdapter extends BaseComposeListAdapter {

	/** section样式id */
	private static final int COMPOSE_STYLE_SECTION = 0;

	/** 单一样式adapter */
	private boolean mIsComposeAdapter = true;

	/**
	 * 构造
	 * 
	 * @param activity
	 */
	public BaseSectionAdapter(Activity activity) {
		super(activity);
	}

	/*
	 * 改写BaseAdapter部分 (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */

	@Override
	public final int getCount() {
		int sectionCount = getSectionCount();
		if (sectionCount <= 0) {
			return 0;
		}

		int count = sectionCount;
		for (int i = 0; i < sectionCount; i++) {
			count += getCount(i);
		}
		return count;
	}

	/**
	 * 获取指定sectionId下的item数量
	 * 
	 * @param sectionId
	 * @return
	 */
	public abstract int getCount(int sectionId);

	/*
	 * 改写BaseAdapter部分 (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */

	@Override
	public final Object getItem(int position) {
		// 分配给各个section
		int sectionCount = getSectionCount();
		for (int i = 0; i < sectionCount; i++) {
			if (position == 0) {
				return COMPOSE_STYLE_SECTION;
			}
			// 去除section项
			position = position - 1;
			if (getCount(i) > position) {
				return getItem(i, position);
			} else {
				position -= getCount(i);
			}
		}
		return null;
	}

	/**
	 * 获取是定sectionId下的某个item
	 * 
	 * @param sectionId
	 * @param position
	 * @return
	 */
	protected abstract Object getItem(int sectionId, int position);

	/*
	 * 改写自BaseComposeListAdapter，对组合样式汇总进行分发 (non-Javadoc)
	 * 
	 * @see
	 * com.ifeng.util.ui.adapter.list.BaseComposeListAdapter#getComposeStyleCount
	 * ()
	 */

	@Override
	protected final int getComposeStyleCount() {
		int sectionCount = getSectionCount();
		if (sectionCount <= 0) {
			return 0;
		}

		// section样式+额外样式
		int sumStyleCount = 1;
		for (int i = 0; i < sectionCount; i++) {
			sumStyleCount += getComposeStyleCount(i);
		}
		return sumStyleCount;
	}

	/*
	 * 改写自BaseComposeListAdapter，对子项item模板view获取进行分发 (non-Javadoc)
	 * 
	 * @see
	 * com.ifeng.util.ui.adapter.list.BaseComposeListAdapter#getComposeStyleCount
	 * ()
	 */

	@Override
	protected final View getComposeItemMouldView(int style) {
		// section样式
		if (style == COMPOSE_STYLE_SECTION) {
			return getSectionMouldView();
		}

		// 去除section样式
		style = style - 1;
		// 分配给各个section
		int sectionCount = getSectionCount();
		for (int i = 0; i < sectionCount; i++) {
			int composeStyleCount = getComposeStyleCount(i);
			if (composeStyleCount > style) {
				if (mIsComposeAdapter) {
					return getComposeItemMouldView(i, style);
				} else {
					return getItemMouldView(i);
				}
			} else {
				style -= composeStyleCount;
			}
		}

		return null;
	}

	@Override
	protected final View getComposeItemView(View convertView, int style,
			int position) {
		int sectionCount = getSectionCount();
		for (int i = 0; i < sectionCount; i++) {
			if (position == 0 && style == COMPOSE_STYLE_SECTION) {
				return getSectionView(convertView, i);
			}
			// 去除section
			position = position - 1;

			if (getCount(i) > position) {
				if (mIsComposeAdapter) {
					return getComposeItemView(convertView, i, style, position);
				} else {
					return getItemView(convertView, i, position);
				}
			} else {
				position -= getCount(i);
			}
		}
		return null;
	}

	@Override
	protected final int getItemComposeStyle(int position) {
		// 分配给各个section
		int style = 0;
		for (int i = 0; i < getSectionCount(); i++) {
			if (position == 0) {
				style = COMPOSE_STYLE_SECTION;
				break;
			}

			// 去除section项
			position = position - 1;
			if (getCount(i) > position) {
				// 当前section中的样式+section样式本身
				style += getItemComposeStyle(i, position) + 1;
				break;
			} else {
				position -= getCount(i);
				style += getComposeStyleCount(i);
			}
		}
		return style;
	}

	/**
	 * 获取指定section下的样式数量
	 * 
	 * @param sectionId
	 * @return
	 */
	protected int getComposeStyleCount(int sectionId) {
		// 缺省为单一样式列表
		mIsComposeAdapter = false;
		return 1;
	}

	/**
	 * 获取指定section下的某一位置样式类型
	 * 
	 * @param sectionId
	 * @param position
	 * @return
	 */
	protected int getItemComposeStyle(int sectionId, int position) {
		// 缺省为单一样式
		mIsComposeAdapter = false;
		return 0;
	}

	/**
	 * 获取指定section下的item模板view，在非组合模式adapter时被调用。
	 * 
	 * @param sectionId
	 * @return
	 */
	protected abstract View getItemMouldView(int sectionId);

	/**
	 * 获取指定section的某一样式模板view，若子类未实现 {@link #getComposeStyleCount}以及
	 * {@link #getItemComposeStyle(int, int)} 的话，则无需针对style进行逻辑判断。
	 * 
	 * @param sectionId
	 * @param style
	 * @return
	 */
	protected View getComposeItemMouldView(int sectionId, int style) {
		// 缺省为单一样式
		if (mIsComposeAdapter) {
			throw new NeedOverrideException(
					"Compose Type need override the getComposeItemMouldView(int sectionId, int style) method");
		}
		return null;
	}

	/**
	 * 获取分栏条数
	 * 
	 * @return
	 */
	protected abstract int getSectionCount();

	/**
	 * 获取指定sectionId下的section name
	 * 
	 * @param sectionId
	 * @return
	 */
	protected String getSectionName(int sectionId) {
		// do nothing
		return String.valueOf(sectionId);
	}

	/**
	 * 获取指定position所处sectionId
	 * 
	 * @param position
	 * @return
	 */
	public final int getSectionId(int position) {
		if (position < 0 || position >= getCount()) {
			return -1;
		}

		for (int i = 0; i < getSectionCount(); i++) {
			if (position == 0) {
				return i;
			}

			// 去除section项
			position = position - 1;
			if (getCount(i) > position) {
				return i;
			} else {
				position -= getCount(i);
			}
		}

		return -1;
	}

	/**
	 * 获取指定position是否为section
	 * 
	 * @param position
	 * @return
	 */
	public final boolean getPositionIsSection(int position) {
		if (position < 0 || position >= getCount()) {
			return false;
		}

		for (int i = 0; i < getSectionCount(); i++) {
			if (position == 0) {
				return true;
			}

			// 去除section项
			position = position - 1;
			if (getCount(i) > position) {
				return false;
			} else {
				position -= getCount(i);
			}
		}

		return false;
	}

	/**
	 * 获取section view的模板view
	 * 
	 * @return
	 */
	public abstract View getSectionMouldView();

	/**
	 * 获取指定section view
	 * 
	 * @param converView
	 * @param sectionId
	 * @return
	 */
	public abstract View getSectionView(View converView, int sectionId);

	/**
	 * 获取指定section下的item view
	 * 
	 * @param converView
	 * @param sectionId
	 * @param position
	 * @return
	 */
	protected abstract View getItemView(View converView, int sectionId,
			int position);

	/**
	 * 获取指定section下的item view，若子类未实现 {@link #getComposeStyleCount}以及
	 * {@link #getItemComposeStyle(int, int)}的话，则无需针对style进行逻辑判断。
	 * 
	 * @param converView
	 * @param sectionId
	 * @param style
	 * @param position
	 * @return
	 */
	protected View getComposeItemView(View converView, int sectionId,
			int style, int position) {
		// 缺省为单一样式
		if (mIsComposeAdapter) {
			throw new NeedOverrideException(
					"Compose Type need override the getComposeItemView(View converView, int sectionId, int style, int position) method");
		}
		return null;
	}
}
