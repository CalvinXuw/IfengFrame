package com.ifeng.util.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 自定义ViewPager解决PhotoView中破坏了touchevent之后，导致onInterceptTouchEvent抛出
 * IllegalArgumentException: pointerIndex out of range.异常的bug。
 * 
 * @author Calvin
 * 
 */
public class IFViewPager extends ViewPager {

	public IFViewPager(Context context) {
		super(context);
	}

	public IFViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		try {
			return super.onInterceptTouchEvent(ev);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		}
	}
}
