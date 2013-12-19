package com.ifeng.util.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

/**
 * 以fragment的形式构成tab 替代原有TabActivity以及TabHost的实现方式
 * 
 * @author Calvin
 * 
 */
public class FragmentTabManager {

	/** fragment manager */
	private FragmentManager mFragmentManager;
	/** 布局id */
	private int mFragmentId;
	/** tab分页fragment */
	private Fragment[] mFragments;
	/** 监听回调 */
	private OnFragmentTabSelectedListener mListener;

	/**
	 * 构造
	 * 
	 * @param fragmentActivity
	 * @param fragmentId
	 *            装载的布局id
	 * @param listener
	 */
	public FragmentTabManager(FragmentActivity fragmentActivity,
			int fragmentId, OnFragmentTabSelectedListener listener) {
		mFragmentManager = fragmentActivity.getSupportFragmentManager();
		mFragmentId = fragmentId;
		mListener = listener;
	}

	/**
	 * 添加tab标签
	 * 
	 * @param fragments
	 */
	public void addTabs(Fragment... fragments) {
		addTabs(0, fragments);
	}

	/**
	 * 添加tab标签，并指定缺省页面
	 * 
	 * @param select
	 * @param fragments
	 */
	public void addTabs(int select, Fragment... fragments) {
		mFragments = fragments;
		setSelectTab(select);
	}

	/**
	 * 设定选中fragment
	 * 
	 * @param position
	 */
	public void setSelectTab(int position) {
		position = Math.max(0, Math.min(mFragments.length - 1, position));
		Fragment currentFragment = mFragmentManager
				.findFragmentById(mFragmentId);
		Fragment selectFragment = mFragments[position];
		if (currentFragment != selectFragment) {
			hideFragmentTab(currentFragment);
			showFragmentTab(selectFragment);
		}

		mListener.onSelected(position);
	}

	/**
	 * 隐藏当前fragment
	 * 
	 * @param fragment
	 */
	private void hideFragmentTab(Fragment fragment) {
		if (fragment == null) {
			return;
		}

		mFragmentManager.beginTransaction().hide(fragment).commit();
		if (fragment instanceof ITabFragmentListener) {
			((ITabFragmentListener) fragment).onTabPause();
		}
	}

	/**
	 * 显示选中的fragment
	 * 
	 * @param fragment
	 */
	private void showFragmentTab(Fragment fragment) {
		if (!mFragmentManager.getFragments().contains(fragment)) {
			mFragmentManager.beginTransaction().add(mFragmentId, fragment)
					.commit();
		} else {
			mFragmentManager.beginTransaction().show(fragment).commit();
			if (fragment instanceof ITabFragmentListener) {
				((ITabFragmentListener) fragment).onTabResume();
			}
		}
	}

	/**
	 * 选项卡变更回调监听，用于装载{@link FragmentTabManager}的Activity进行选项卡View切换显示状态的回调
	 * 
	 * @author Calvin
	 * 
	 */
	public interface OnFragmentTabSelectedListener {

		/**
		 * 选项卡发生变化
		 * 
		 * @param position
		 */
		public void onSelected(int position);
	}

	/**
	 * 由于FragmentTransaction.hide(Fragment)以及
	 * FragmentTransaction#show(Fragment)方法
	 * 方法不会触发当前显示的Fragment生命周期发生变化，为了完善处理逻辑，可令Fragment实现此接口
	 * 辅助FragmentTabManager完成对Fragment生命周期逻辑的补充实现。
	 * 
	 * @author Calvin
	 * 
	 */
	public interface ITabFragmentListener {

		/**
		 * 拟onPause()
		 */
		public void onTabPause();

		/**
		 * 拟onResume()
		 */
		public void onTabResume();

	}
}
