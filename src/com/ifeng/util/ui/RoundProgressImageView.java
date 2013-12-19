package com.ifeng.util.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.ifeng.util.Utility;
import com.ifeng.util.imagecache.ProgressImageView;

/**
 * @author qianzy
 * @time 2013-7-11 09:49:12
 * @describe 带百分比的圆形progressbar
 */
public class RoundProgressImageView extends ProgressImageView {

	public RoundProgressImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private int mProgressBarWidth = 10;
	private int mBackgroundWidth = 15;
	private int mTextSize = 15;

	private int mProgressColor = 0xFFAAAAAA;
	private int mProgressBackgroundColor = 0xFF1E1E1E;
	private int mBackgroundColor = 0xFF1A1A1A;
	private int mTextColor = 0xFFAAAAAA;

	private Paint mProgressPaint = new Paint();
	private Paint mProgressBackgroundPaint = new Paint();
	private Paint mBackgroundPaint = new Paint();
	private Paint textPaint = new Paint();

	private RectF mBackgroundBounds = new RectF();
	private RectF mProgressBounds = new RectF();

	@Override
	public void onAttachedToWindow() {
		mProgressBarWidth = (int) (mProgressBarWidth * Utility
				.getScaledDensity(getContext()));
		mBackgroundWidth = (int) (mBackgroundWidth * Utility
				.getScaledDensity(getContext()));
		mTextSize = (int) (mTextSize * Utility.getScaledDensity(getContext()));

		super.onAttachedToWindow();
		setupPaints();
		invalidate();
	}

	/***
	 * 初始化画笔
	 */
	private void setupPaints() {

		mProgressPaint.setColor(mProgressColor);
		mProgressPaint.setAntiAlias(true);
		mProgressPaint.setStyle(Style.STROKE);
		mProgressPaint.setStrokeWidth(mProgressBarWidth);

		mProgressBackgroundPaint.setColor(mProgressBackgroundColor);
		mProgressBackgroundPaint.setAntiAlias(true);
		mProgressBackgroundPaint.setStyle(Style.STROKE);
		mProgressBackgroundPaint.setStrokeWidth(mProgressBarWidth);

		mBackgroundPaint.setColor(mBackgroundColor);
		mBackgroundPaint.setAntiAlias(true);
		mBackgroundPaint.setStyle(Style.FILL);
		mBackgroundPaint.setStrokeWidth(mBackgroundWidth);

		textPaint.setColor(mTextColor);
		textPaint.setStyle(Style.FILL);
		textPaint.setAntiAlias(true);
		textPaint.setTextSize(mTextSize);
	}

	/***
	 * 设置边界
	 */
	private void setupBounds() {

		int fullDiameter = (getWidth() > getHeight() ? getHeight() : getWidth()) / 2;
		int backgroundRadius = fullDiameter * 2 / 5;
		int progressRadius = fullDiameter / 4;

		mBackgroundBounds = new RectF(getWidth() / 2 - backgroundRadius,
				getHeight() / 2 - backgroundRadius, getWidth() / 2
						+ backgroundRadius, getHeight() / 2 + backgroundRadius);
		mProgressBounds = new RectF(getWidth() / 2 - progressRadius,
				getHeight() / 2 - progressRadius, getWidth() / 2
						+ progressRadius, getHeight() / 2 + progressRadius);
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mProgress == DEFAULT_PROGRESS || mProgress == 100) {
			return;
		}
		setupBounds();
		// 绘制背景
		canvas.drawArc(mBackgroundBounds, 360, 360, false, mBackgroundPaint);
		// 绘制进度条背景
		canvas.drawArc(mProgressBounds, 360, 360, false,
				mProgressBackgroundPaint);
		// 绘制进度条
		canvas.drawArc(mProgressBounds, -90, mProgress * (360f / 100), false,
				mProgressPaint);

		/** 绘制圆里面的文字 */
		String progressStr = mProgress + "%";
		float offset = textPaint.measureText(progressStr) / 2;
		FontMetrics fontMetrics = textPaint.getFontMetrics();
		canvas.drawText(progressStr, getWidth() / 2 - offset, getHeight() / 2
				+ fontMetrics.descent, textPaint);
	}

}
