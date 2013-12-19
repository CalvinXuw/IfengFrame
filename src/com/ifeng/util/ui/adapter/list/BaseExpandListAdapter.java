package com.ifeng.util.ui.adapter.list;

import android.app.Activity;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;

import com.ifeng.util.ui.IFRefreshListView;

/**
 * 分组展示的adapter，用于展示类似联系人分组、内容分组等样式。继承自{@link BaseSectionAdapter}可通过
 * {@link #expandUnit(int)}以及{@link #shrinkUnit(int)}方法控制分组的展开与收拢。
 * 
 * @author Calvin
 * 
 */
public abstract class BaseExpandListAdapter extends BaseSectionAdapter {

	/** 单元数据状态记录 */
	private SparseArray<ExpandUnitState> mExpandUnits;
	/** 状态通知回调 */
	private ExpandListAdapterListener mExpandAdapterListener;

	/**
	 * 构造
	 * 
	 * @param activity
	 * @param listener
	 */
	public BaseExpandListAdapter(Activity activity,
			ExpandListAdapterListener listener) {
		this(activity);
		mExpandAdapterListener = listener;
	}

	/**
	 * 私有化原有父类构造方法
	 * 
	 * @param activity
	 */
	private BaseExpandListAdapter(Activity activity) {
		super(activity);
		mExpandUnits = new SparseArray<ExpandUnitState>();
	}

	/**
	 * 展开分组
	 * 
	 * @param sectionId
	 */
	public void expandUnit(int sectionId) {
		expandUnit(sectionId, null);
	}

	/**
	 * 收起分组
	 * 
	 * @param sectionId
	 */
	public void shrinkUnit(int sectionId) {
		shrinkUnit(sectionId, null);
	}

	/**
	 * 展开分组
	 * 
	 * @param sectionId
	 * @param sectionView
	 */
	private void expandUnit(int sectionId, View sectionView) {
		ExpandUnitState state = getExpandUnitState(sectionId);
		if (!state.mIsExpand) {
			if (mExpandAdapterListener.isSingleExpand()) {
				for (int i = 0; i < mExpandUnits.size(); i++) {
					ExpandUnitState eachState = mExpandUnits.get(i);
					if (eachState.mIsExpand) {
						shrinkUnit(i);
						break;
					}
				}
			}
			state.mIsExpand = true;
			notifyDataSetChanged();
			mExpandAdapterListener.onUnitExpand(sectionId, sectionView);
		}
	}

	/**
	 * 收起分组
	 * 
	 * @param sectionId
	 * @param sectionView
	 */
	private void shrinkUnit(int sectionId, View sectionView) {
		ExpandUnitState state = getExpandUnitState(sectionId);
		if (state.mIsExpand) {
			state.mIsExpand = false;
			notifyDataSetChanged();
			mExpandAdapterListener.onUnitShrink(sectionId, sectionView);
		}
	}

	/**
	 * 获取分组状态信息，由此方法处理信息采集工作
	 * 
	 * @param sectionId
	 * @return
	 */
	private ExpandUnitState getExpandUnitState(int sectionId) {
		ExpandUnitState record = mExpandUnits.get(sectionId);

		// 尚未录入
		if (record == null) {
			record = new ExpandUnitState();
			record.mIsExpand = false;
			record.mUnitChildCount = getExpandCount(sectionId);
			mExpandUnits.put(sectionId, record);
		}

		return record;
	}

	@Override
	public final int getCount(int sectionId) {
		ExpandUnitState record = getExpandUnitState(sectionId);

		if (record.mIsExpand) {
			return record.mUnitChildCount;
		} else {
			return 0;
		}
	}

	@Override
	protected final int getSectionCount() {
		return getExpandSectionCount();
	}

	@Override
	public final View getSectionMouldView() {
		return getExpandMouldView();
	}

	@Override
	public final View getSectionView(View converView, final int sectionId) {
		View expandView = getExpandView(converView, sectionId);
		expandView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ExpandUnitState state = mExpandUnits.get(sectionId);
				if (state.mIsExpand) {
					shrinkUnit(sectionId, v);
				} else {
					expandUnit(sectionId, v);
				}
			}

		});
		return expandView;
	}

	/**
	 * 获取展开分组之中的子项数量
	 * 
	 * @param sectionId
	 * @return
	 */
	public abstract int getExpandCount(int sectionId);

	/**
	 * 获取分组数量
	 * 
	 * @return
	 */
	public abstract int getExpandSectionCount();

	/**
	 * 获取展开分组模板View
	 * 
	 * @return
	 */
	public abstract View getExpandMouldView();

	/**
	 * 获取指定的展开分组view
	 * 
	 * @param conView
	 * @param sectionId
	 * @return
	 */
	public abstract View getExpandView(View convertView, int sectionId);

	/**
	 * 存储某一单元数据状态
	 * 
	 * @author Calvin
	 * 
	 */
	private class ExpandUnitState {

		/** 该单元内中的子列表项数量 */
		private int mUnitChildCount;
		/** 是否展开 */
		private boolean mIsExpand;

	}

	/**
	 * 展开单元的回调接口，通知某一单元展开、收起动作触发，等
	 * 
	 * @author Calvin
	 * 
	 */
	public interface ExpandListAdapterListener {

		/**
		 * 是否为单一单元展开模式
		 * 
		 * @return
		 */
		public boolean isSingleExpand();

		/**
		 * 展开回调，可用于执行动画<br>
		 * 可在回调中执行{@link IFRefreshListView#setSelectSection(int)}
		 * 方法矫正ListView滚动位置
		 * 
		 * @param position
		 * @param sectionView
		 */
		public void onUnitExpand(int position, View sectionView);

		/**
		 * 收起回调，可用于执行动画，注意：其中sectionView可能为空<br>
		 * 可在回调中执行{@link IFRefreshListView#setSelectSection(int)}
		 * 方法矫正ListView滚动位置
		 * 
		 * @param position
		 * @param sectionView
		 */
		public void onUnitShrink(int position, View sectionView);
	}
}
