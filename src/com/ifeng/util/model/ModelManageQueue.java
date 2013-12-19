package com.ifeng.util.model;

import java.util.HashMap;
import java.util.Iterator;

import android.text.TextUtils;

/**
 * model管理队列，方便对复合的model进行管理
 * 
 * @author Calvin
 * 
 */
public class ModelManageQueue {

	/** model map */
	private HashMap<String, AbstractModel> mModelMap;

	/**
	 * 构造
	 */
	public ModelManageQueue() {
		mModelMap = new HashMap<String, AbstractModel>();
	}

	/**
	 * 添加任务到队列
	 * 
	 * @param key
	 * @param model
	 */
	public void addTaskModel(String key, AbstractModel model) {
		synchronized (mModelMap) {
			if (!TextUtils.isEmpty(key) && model != null) {
				mModelMap.put(key, model);
			}
		}
	}

	/**
	 * 根据model key获取一个model
	 * 
	 * @param key
	 * @return
	 */
	public AbstractModel getTaskModel(String key) {
		synchronized (mModelMap) {
			return mModelMap.get(key);
		}
	}

	/**
	 * 根据model key取消一个进行中或者等待进行的model
	 * 
	 * @param model
	 */
	public void cancelTaskModel(String key) {
		synchronized (mModelMap) {
			AbstractModel model = mModelMap.get(key);
			if (model != null) {
				model.cancel();
			}
		}
	}

	/**
	 * 根据model key暂停或者继续一个进行中的model
	 * 
	 * @param key
	 * @param isPause
	 */
	public void pauseTaskModel(String key, boolean isPause) {
		synchronized (mModelMap) {
			AbstractModel model = mModelMap.get(key);
			if (model != null) {
				model.setPause(isPause);
			}
		}
	}

	/**
	 * 暂停或继续队列任务
	 * 
	 * @param isPause
	 */
	public void pauseQueue(boolean isPause) {
		synchronized (mModelMap) {
			for (Iterator<String> iterator = mModelMap.keySet().iterator(); iterator
					.hasNext();) {
				String key = (String) iterator.next();
				AbstractModel model = mModelMap.get(key);
				if (model != null) {
					model.setPause(isPause);
				}
			}
		}
	}

	/**
	 * 取消全部任务
	 */
	public void clearQueue() {
		synchronized (mModelMap) {
			for (Iterator<String> iterator = mModelMap.keySet().iterator(); iterator
					.hasNext();) {
				String key = (String) iterator.next();
				AbstractModel model = mModelMap.get(key);
				if (model != null) {
					model.cancel();
				}
			}
			mModelMap.clear();
		}
	}
}
