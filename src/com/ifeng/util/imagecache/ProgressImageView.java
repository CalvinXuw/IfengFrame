package com.ifeng.util.imagecache;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * 显示进度条的ImageView
 * 
 * @author Calvin
 * 
 */
public abstract class ProgressImageView extends ImageView {

	/** 初始进度 */
	protected static final int DEFAULT_PROGRESS = -1;

	/** 进度 */
	protected int mProgress = DEFAULT_PROGRESS;

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public ProgressImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/***
	 * 重置进度
	 */
	public void resetProgress() {
		mProgress = DEFAULT_PROGRESS;
		invalidate();
	}

	/***
	 * 设置进度
	 * 
	 * @param progress
	 */
	public void setProgress(int progress) {
		mProgress = progress;
		invalidate();
	}
}
