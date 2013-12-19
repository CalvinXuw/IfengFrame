package com.ifeng.util.ui;

import android.view.View;

/**
 * 针对{@link View#dispatchTouchEvent(android.view.MotionEvent)}
 * 方法中实现主动事件分发的View的补充实现
 * 
 * @author Calvin
 * 
 */
public interface ISelfDispatchEventDelegate {

	/**
	 * 配合{@link #requestDisallowInterceptTouchEvent(boolean)}方法实现控制手动分配事件的开关
	 * 
	 * @param enable
	 */
	public void setSelfDispatchEnable(boolean enable);

}
