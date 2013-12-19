package com.ifeng.util.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.ifeng.BaseApplicaion;

/**
 * 页面状态的控制类
 * 
 * @author XuWei
 * 
 */
public class StateView extends FrameLayout {

	/**
	 * 构造
	 * 
	 * @param context
	 */
	public StateView(Context context) {
		super(context);
		init();
	}

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 */
	public StateView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * 构造
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public StateView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	/**
	 * 初始化
	 */
	private void init() {

	}

	/**
	 * 加载一条状态
	 * 
	 * @param state
	 */
	public void setState(State state) {
		if (state == null) {
			throw new IllegalArgumentException("current state can not be null");
		}

		View stateView = state.getStateView();
		removeAllViews();
		addView(stateView);
		setVisibility(VISIBLE);
	}

	/**
	 * 关闭StateView
	 */
	public void dismiss() {
		setVisibility(GONE);
		removeAllViews();
	}

	/**
	 * 状态信息及操作响应承载类
	 * 
	 * @author XuWei
	 * 
	 */
	public static abstract class State {

		/** Tag */
		protected final String TAG = getClass().getSimpleName();
		/** if enabled, logcat will output the log. */
		protected static final boolean DEBUG = true & BaseApplicaion.DEBUG;
		/** Activity */
		protected Activity mActivity;
		/** 状态View */
		private View mStateView;

		public State(Activity activity) {
			mActivity = activity;
		}

		/**
		 * 生成状态View
		 */
		protected abstract View createView();

		/**
		 * 添加对于事件处理
		 */
		protected abstract void addAction(View container);

		/**
		 * 获取状态显示View
		 * 
		 * @return
		 */
		protected View getStateView() {
			if (mStateView == null) {
				mStateView = createView();
				addAction(mStateView);
			}
			return mStateView;
		}

		/**
		 * StateView之中事件触发的监听
		 * 
		 * @author XuWei
		 * 
		 */
		public interface OnStateActionListener {

			/**
			 * 事件触发
			 * 
			 * @param actionId
			 *            事件id定义于实现类中
			 */
			public void onActionTrigger(int actionId);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
	}
}
