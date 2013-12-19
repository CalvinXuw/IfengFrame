package com.ifeng.util.ui.adapter;

import java.util.List;

import com.ifeng.util.ui.SlideTabbarView;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ViewGroup;

/**
 * FragmentPagerAdapter和OnPageChangeListener的连接Adapter
 * 
 * @author Calvin
 * 
 */
public class PagerSliderConnectAdapter extends FragmentPagerAdapter implements
		OnPageChangeListener {

	/** 资源fragments */
	private List<Fragment> mFragments;
	/** 滑块 */
	private SlideTabbarView mSlideTabbar;

	public PagerSliderConnectAdapter(FragmentManager fm,
			List<Fragment> fragments, SlideTabbarView slideTabbar) {
		super(fm);
		mFragments = fragments;
		mSlideTabbar = slideTabbar;
	}

	@Override
	public int getCount() {
		return mFragments.size();
	}

	@Override
	public Fragment getItem(int position) {
		return mFragments.get(position);
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
	}

	@Override
	public void onPageSelected(int arg0) {
		mSlideTabbar.setSelect(arg0);
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}
}