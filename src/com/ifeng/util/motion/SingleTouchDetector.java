package com.ifeng.util.motion;

import android.view.MotionEvent;

/**
 * 单指触摸事件捕捉分析器，用于辅助界面完成对触摸事件位移、位置等的数据支撑。
 * 
 * @author Calvin
 * 
 */
public class SingleTouchDetector {

	/** 默认初始位置 */
	private static final int DEFAULT_POSITION = -1;

	/** 是否自动进行动作捕捉 */
	private boolean mIsAutoDispatch;
	/** 是否捕捉了事件 */
	private boolean mIsDispatched;

	/** 事件捕捉时间 */
	private long mEventStartTime;
	/** 起始位置记录X */
	private float mEventStartX = DEFAULT_POSITION;
	/** 起始位置记录Y */
	private float mEventStartY = DEFAULT_POSITION;
	/** 上一次位置记录X */
	private float mEventLastX = DEFAULT_POSITION;
	/** 上一次位置记录Y */
	private float mEventLastY = DEFAULT_POSITION;
	/** 获取后两次触摸事件相差横坐标距离 */
	private float mEventDx;
	/** 获取后两次触摸事件相差纵坐标距离 */
	private float mEventDy;

	/**
	 * 构造
	 */
	public SingleTouchDetector() {
		this(true);
	}

	/**
	 * 构造
	 * 
	 * @param isAutoDispatch
	 *            是否为自动匹配扑捉动作
	 */
	public SingleTouchDetector(boolean isAutoDispatch) {
		mIsAutoDispatch = isAutoDispatch;
	}

	/**
	 * 新增一个新的触摸事件
	 * 
	 * @param ev
	 */
	public void addMotion(MotionEvent ev) {
		if (!mIsDispatched) {
			if (mIsAutoDispatch) {
				startDispatch(ev);
			} else {
				return;
			}
		} else {
			int actionId = ev.getAction();

			switch (actionId) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				if (ev.getHistorySize() > 1) {
					mEventDx = mEventLastX - ev.getX();
					mEventDy = mEventLastY - ev.getY();
				}

				mEventLastX = ev.getX();
				mEventLastY = ev.getY();

				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				abandonDispatch();
				break;
			}
		}
	}

	/**
	 * 开始捕捉记录
	 * 
	 * @param ev
	 */
	public void startDispatch(MotionEvent ev) {
		mIsDispatched = true;
		mEventStartTime = System.currentTimeMillis();
		mEventStartX = ev.getX();
		mEventStartY = ev.getY();
	}

	/**
	 * 结束捕捉记录
	 */
	public void abandonDispatch() {
		mIsDispatched = false;
		mEventStartTime = 0;
		mEventStartX = DEFAULT_POSITION;
		mEventStartY = DEFAULT_POSITION;
		mEventLastX = DEFAULT_POSITION;
		mEventLastY = DEFAULT_POSITION;
		mEventDx = 0;
		mEventDy = 0;
	}

	/**
	 * 获取后两次触摸事件相差横向距离
	 * 
	 * @return
	 */
	public float getLastDx() {
		return mEventDx;
	}

	/**
	 * 获取后两次触摸事件相差纵向距离
	 * 
	 * @param ev
	 * @return
	 */
	public float getLastDy() {
		return mEventDy;
	}

	/**
	 * 获取横向位移距离
	 * 
	 * @return
	 */
	public float getMovingDistanceX() {
		if (mIsDispatched && mEventStartX != DEFAULT_POSITION
				&& mEventLastX != DEFAULT_POSITION) {
			float distance = mEventLastX - mEventStartX;
			return distance;
		}
		return 0;
	}

	/**
	 * 获取纵向位移距离
	 * 
	 * @return
	 */
	public float getMovingDistanceY() {
		if (mIsDispatched && mEventStartY != DEFAULT_POSITION
				&& mEventLastY != DEFAULT_POSITION) {
			float distance = mEventLastY - mEventStartY;
			return distance;
		}
		return 0;
	}

	/**
	 * 获取两点间距离
	 * 
	 * @return
	 */
	public float getMovingDistance() {
		if (mIsDispatched && mEventStartX != DEFAULT_POSITION
				&& mEventLastX != DEFAULT_POSITION) {
			float distance = (float) Math.sqrt(Math.pow(mEventLastX
					- mEventStartX, 2)
					+ Math.pow(mEventLastY - mEventStartY, 2));
			return distance;
		}
		return 0;
	}

	/**
	 * 获取触摸事件发生时间
	 * 
	 * @return
	 */
	public long getEventStartTime() {
		return mEventStartTime;
	}

	/**
	 * 是否捕捉到触摸事件
	 * 
	 * @return
	 */
	public boolean isTouchEventDispatched() {
		return mIsDispatched;
	}

	/**
	 * 根据业务逻辑调整初始坐标位置
	 * 
	 * @param dx
	 * @param dy
	 */
	public void changeEventStartPointBy(int dx, int dy) {
		if (mIsDispatched) {
			mEventStartX += dx;
			mEventStartY += dy;
		}
	}
}
