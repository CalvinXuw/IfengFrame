package com.ifeng.util.ui.anim;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;

/**
 * 翻转动画控制类
 * 
 * @author Calvin
 * 
 */
public class Rotate3dAnimation implements AnimationListener {

	/** 翻转角度 */
	private static final int ROTATE_DEGREE = 90;

	/** 动画执行view */
	private View mAnimView;
	/** 动画翻转监听 */
	private OnRotateListener mOnRotateListener;
	/** 翻转方向 */
	private boolean mIsVerticalRotate;

	/**
	 * 构造
	 * 
	 * @param view
	 * @param listener
	 */
	public Rotate3dAnimation(View view, boolean isVertical,
			OnRotateListener listener) {
		mAnimView = view;
		mOnRotateListener = listener;
	}

	/**
	 * 进行翻转动画
	 */
	public void rotate() {
		// 计算中心点
		float centerX = mAnimView.getMeasuredWidth() / 2.0f;
		float centerY = mAnimView.getMeasuredHeight() / 2.0f;

		RotateAnimation rotation = new RotateAnimation(0, -ROTATE_DEGREE,
				centerX, centerY, 0, false);
		rotation.setIsVerticalRotate(mIsVerticalRotate);
		rotation.setDuration(1500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(this);

		mAnimView.startAnimation(rotation);
	}

	@Override
	public final void onAnimationEnd(Animation animation) {
		mOnRotateListener.onRotateFinish(mAnimView);

		// 计算中心点
		float centerX = mAnimView.getMeasuredWidth() / 2.0f;
		float centerY = mAnimView.getMeasuredHeight() / 2.0f;

		// 进行返回动画
		RotateAnimation rotation = new RotateAnimation(ROTATE_DEGREE, 0,
				centerX, centerY, 0, true);
		rotation.setIsVerticalRotate(mIsVerticalRotate);
		rotation.setDuration(1500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(this);

		mAnimView.startAnimation(rotation);
	}

	/**
	 * 翻转动画具体实现
	 * 
	 * @author Calvin
	 * 
	 */
	private class RotateAnimation extends Animation {
		/** 开始角度 */
		private final float mFromDegrees;
		/** 结束角度 */
		private final float mToDegrees;
		/** 中心点 x轴位置 */
		private final float mCenterX;
		/** 中心点 y轴位置 */
		private final float mCenterY;
		/** 景深变化 */
		private final float mDepthZ;
		/** 是否为反转动画 */
		private final boolean mIsReverse;
		/** 摄像头 */
		private Camera mCamera;

		/** 水平反转或者垂直反转 */
		private boolean mIsVerticalRotate;

		/**
		 * 构造
		 * 
		 * @param fromDegrees
		 * @param toDegrees
		 * @param centerX
		 * @param centerY
		 * @param depthZ
		 * @param reverse
		 */
		public RotateAnimation(float fromDegrees, float toDegrees,
				float centerX, float centerY, float depthZ, boolean reverse) {
			mFromDegrees = fromDegrees;
			mToDegrees = toDegrees;
			mCenterX = centerX;
			mCenterY = centerY;
			mDepthZ = depthZ;
			mIsReverse = reverse;
		}

		@Override
		public void initialize(int width, int height, int parentWidth,
				int parentHeight) {
			super.initialize(width, height, parentWidth, parentHeight);
			mCamera = new Camera();
		}

		// 生成Transformation
		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation t) {
			final float fromDegrees = mFromDegrees;
			// 生成中间角度
			float degrees = fromDegrees
					+ ((mToDegrees - fromDegrees) * interpolatedTime);

			final float centerX = mCenterX;
			final float centerY = mCenterY;
			final Camera camera = mCamera;

			final Matrix matrix = t.getMatrix();

			camera.save();
			if (mIsReverse) {
				camera.translate(0.0f, 0.0f, mDepthZ
						* (1.0f - interpolatedTime));
			} else {
				camera.translate(0.0f, 0.0f, mDepthZ * interpolatedTime);
			}

			if (mIsVerticalRotate) {
				camera.rotateY(degrees);
			} else {
				camera.rotateX(degrees);
			}

			// 取得变换后的矩阵
			camera.getMatrix(matrix);
			camera.restore();

			matrix.preTranslate(-centerX, -centerY);
			matrix.postTranslate(centerX, centerY);
		}

		/**
		 * 设置水平翻转or垂直翻转
		 * 
		 * @param isVertical
		 */
		private void setIsVerticalRotate(boolean isVertical) {
			mIsVerticalRotate = isVertical;
		}
	}

	/**
	 * 翻转动画监听
	 * 
	 * @author Calvin
	 * 
	 */
	public interface OnRotateListener {

		/** 翻转动画过半，需要更新界面状态 */
		public void onRotateFinish(View view);
	}

	@Override
	public void onAnimationStart(Animation animation) {

	}

	@Override
	public void onAnimationRepeat(Animation animation) {

	}
}
