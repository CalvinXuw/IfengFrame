package com.ifeng.util.net.requestor;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;

import android.content.Context;

import com.ifeng.util.net.requestor.RequestTask.OnRequestTaskListener;
import com.ifeng.util.net.requestor.UploadRequestTask.FileValuePair;

/**
 * 文件上传基类
 * 
 * @author Calvin
 * 
 */
public abstract class BaseUploadFileRequestor extends AbstractRequestor {

	/**
	 * 构造
	 * 
	 * @param context
	 * @param listener
	 */
	public BaseUploadFileRequestor(Context context,
			OnModelProcessListener listener) {
		super(context, listener);
	}

	@Override
	protected void process() {
		turnOffPreloadFromCache();
		mRequestDataCache = null;
		super.process();
	}

	@Override
	protected WebRequestTask getWebRequestTask(
			OnRequestTaskListener webRequestTaskListener) {
		UploadRequestTask uploadRequestTask = new UploadRequestTask(mContext,
				getAdjustRequestUrl(), getRequestHeaders(), getCombineParams(),
				webRequestTaskListener);
		return uploadRequestTask;
	}

	@Override
	protected final List<NameValuePair> getRequestParams() {
		List<NameValuePair> strings = getRequestStringParams();
		List<FileValuePair> files = getRequestFileParams();
		List<NameValuePair> combine = new LinkedList<NameValuePair>();
		if (strings != null) {
			combine.addAll(strings);
		}
		if (files != null) {
			combine.addAll(files);
		}
		return combine;
	}

	/**
	 * 获取字符串类型的参数
	 * 
	 * @return
	 */
	protected abstract List<NameValuePair> getRequestStringParams();

	/**
	 * 获取上传文件参数
	 * 
	 * @return
	 */
	protected abstract List<FileValuePair> getRequestFileParams();

}
