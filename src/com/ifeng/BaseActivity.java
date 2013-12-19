package com.ifeng;

import java.util.LinkedList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.ifeng.android.R;
import com.ifeng.util.SdkVersionUtils;
import com.ifeng.util.model.AbstractModel;
import com.ifeng.util.model.AbstractModel.OnModelProcessListener;
import com.ifeng.util.model.ModelManageQueue;

/**
 * BaseActivity所有实现的Activity页面应将继承自此类
 * 
 * @author Xuwei
 * 
 */
public abstract class BaseActivity extends FragmentActivity implements
		OnModelProcessListener {

	/** tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug开关 */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;
	/**
	 * 窗口栈。
	 */
	public static LinkedList<BaseActivity> sActivityStack = new LinkedList<BaseActivity>();

	/** model管理类 */
	protected ModelManageQueue mModelManageQueue;

	/** 跳转至下一页面的离开动画 */
	private int mPushLeftAnim = R.anim.activity_anim_push_left_out;
	/** 跳转至下一页面的进入动画 */
	private int mPushInAnim = R.anim.activity_anim_push_left_in;

	/** 返回至上一页面的离开动画 */
	private int mPopLeftAnim = R.anim.activity_anim_push_right_out;
	/** 返回至上一页面的进入动画 */
	private int mPopInAnim = R.anim.activity_anim_push_right_in;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		sActivityStack.add(this);
		mModelManageQueue = new ModelManageQueue();
	}

	@Override
	protected void onResume() {
		// 移到顶端。
		sActivityStack.remove(this);
		sActivityStack.add(this);
		mModelManageQueue.pauseQueue(false);
		super.onResume();
	}

	@Override
	protected void onPause() {
		mModelManageQueue.pauseQueue(true);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		sActivityStack.remove(this);
		System.gc();
		mModelManageQueue.clearQueue();
		super.onDestroy();
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		if (SdkVersionUtils.hasFroyo()) {
			overridePendingTransition(mPushInAnim, mPushLeftAnim);
		}
	}

	@Override
	public void finish() {
		sActivityStack.remove(this);
		super.finish();
		if (SdkVersionUtils.hasFroyo()) {
			overridePendingTransition(mPopInAnim, mPopLeftAnim);
		}
	}

	/**
	 * 提供原始finish方法
	 */
	public final void baseFinish() {
		super.finish();
	}

	/**
	 * 获取栈顶的activity
	 * 
	 * @return
	 */
	public static Activity getTopActivity() {
		if (sActivityStack.size() > 0) {
			return sActivityStack.getLast();
		}
		return null;
	}

	/**
	 * 清空activity栈
	 */
	public static void clearStack() {
		while (!sActivityStack.isEmpty()) {
			sActivityStack.poll().baseFinish();
		}
		sActivityStack.clear();
	}

	@Override
	public void onSuccess(AbstractModel model) {
		// do nothing
	}

	@Override
	public void onFailed(AbstractModel model, int errorCode) {
		// do nothing
	}

	@Override
	public void onProgress(AbstractModel model, int progress) {
		// do nothing
	}

	/**
	 * 设置跳转动画的驶离动画
	 * 
	 * @param pushLeftAnim
	 */
	protected void setPushLeftAnim(int pushLeftAnim) {
		this.mPushLeftAnim = pushLeftAnim;
	}

	/**
	 * 设置跳转动画的驶入动画
	 * 
	 * @param pushInAnim
	 */
	protected void setPushInAnim(int pushInAnim) {
		this.mPushInAnim = pushInAnim;
	}

	/**
	 * 设置返回动画的驶离动画
	 * 
	 * @param popLeftAnim
	 */
	protected void setPopLeftAnim(int popLeftAnim) {
		this.mPopLeftAnim = popLeftAnim;
	}

	/**
	 * 设置返回动画的驶入动画
	 * 
	 * @param popInAnim
	 */
	protected void setPopInAnim(int popInAnim) {
		this.mPopInAnim = popInAnim;
	}

}
