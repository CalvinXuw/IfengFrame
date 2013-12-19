package com.ifeng.util.net.requestor;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;

import android.content.Context;

import com.ifeng.util.logging.Log;
import com.ifeng.util.model.AbstractModel;
import com.ifeng.util.net.parser.AbstractIFItem;
import com.ifeng.util.net.requestor.RequestTask.OnRequestTaskListener;
import com.ifeng.util.net.requestor.WebRequestTask.RequestType;

/**
 * Requestor的基类继承自{@link AbstractModel}，所有的接口Requestor本质上都从它继承
 * 从Requestor中获取网络访问信息，并解析得到的数据
 * 执行request()方法开始异步获取数据，并由OnRequestorListener监听请求结果状态
 * 
 * @author xuwei
 * 
 */
public abstract class AbstractRequestor extends AbstractModel {

	/** Http地址的前缀 */
	private static final String HTTP_URL_PREFIX = "http://";
	/** Http地址的前缀长度 */
	private static final int HTTP_URL_PREFIX_LENGTH = HTTP_URL_PREFIX.length();

	/** 数据请求Task */
	private RequestTask mRequestTask;

	/** 参数提交试，默认Post提交 */
	private RequestType mRequestType = RequestType.POST;

	/** Web数据请求TaskListener */
	private OnRequestTaskListener mWebOnRequestTaskListener;

	/** Cache数据请求TaskListener */
	private OnRequestTaskListener mCacheOnRequestTaskListener;

	/** Preload数据请求TaskListener */
	private OnRequestTaskListener mPreloadOnRequestTaskListener;

	/** 获取cache listener */
	private OnPreloadFromCacheListener mOnPreloadFromCacheListener;

	/** Cache 工具 */
	protected RequestDataCache mRequestDataCache;

	/** 是否需要从历史缓存中预加载 */
	private boolean mNeedPreload;

	/** 是否已经从历史缓存中预加载 */
	private boolean mIsPreloaded;

	/** 自动解析的Class类型 */
	private Class<? extends AbstractIFItem> mAutoParseClass;

	/**
	 * 构造
	 * 
	 * @param context
	 * @param listener
	 */
	public AbstractRequestor(Context context, OnModelProcessListener listener) {
		super(context, listener);
	}

	/**
	 * 初始化Web请求的Listener，及其它各项必要内容
	 */
	@Override
	protected void init() {
		if (DEBUG) {
			Log.d(TAG, "requestor has init");
		}

		// 针对于网络请求任务的回调
		mWebOnRequestTaskListener = new OnRequestTaskListener() {

			@Override
			public void onSuccess(String result) {
				if (DEBUG) {
					Log.d(TAG, "webtask request successed");
				}

				// 若网络请求在preload之前，则取消preload
				if (!isPreloaded()) {
					turnOffPreloadFromCache();
				}

				// 解析数据
				parseResult(result, false);

				cacheDataIfNeed(result);

				AbstractRequestor.this.onSuccess();
			}

			@Override
			public void onFailed(final int errorCode) {
				if (DEBUG) {
					Log.d(TAG, "webtask request failed");
				}

				AbstractRequestor.this.onFailed(errorCode);
			}

			@Override
			public void onProgress() {
			}
		};

		// 针对于缓存请求任务的回调
		mCacheOnRequestTaskListener = new OnRequestTaskListener() {

			@Override
			public void onSuccess(String result) {
				if (DEBUG) {
					Log.d(TAG, "cachetask request successed");
				}

				// 解析数据
				parseResult(result, false);

				// 回调
				AbstractRequestor.this.onSuccess();
			}

			@Override
			public void onFailed(int errorCode) {
				if (DEBUG) {
					Log.d(TAG, "cachetask request failed , retry with webtask");
				}

				// 若从缓存中读取数据失败，则清除失效缓存，并重新发起网络请求
				mRequestDataCache.delete();
				process();
			}

			@Override
			public void onProgress() {
			}
		};

		// 预加载数据请求任务的回调
		mPreloadOnRequestTaskListener = new OnRequestTaskListener() {

			@Override
			public void onSuccess(String result) {
				if (DEBUG) {
					Log.d(TAG, "preload from exist cache successed");
				}

				parseResult(result, true);

				// 此请求已经被Cancel
				if (!processCanMoveOn()) {
					return;
				}

				mIsPreloaded = true;

				// 回调
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						if (mOnPreloadFromCacheListener != null) {
							mOnPreloadFromCacheListener
									.onCacheLoaded(AbstractRequestor.this);
							turnOffPreloadFromCache();
						}
					}
				});

				process();
			}

			@Override
			public void onFailed(int errorCode) {
				if (DEBUG) {
					Log.d(TAG, "preload from exist cache failed");
				}

				// 若从缓存中读取数据失败，则清除失效缓存，并重新发起网络请求
				mRequestDataCache.delete();
				process();
			}

			@Override
			public void onProgress() {
			}
		};
	}

	@Override
	protected void process() {
		mRequestTask = null;

		if (canUseCache()) {
			fillCacheFileTag();

			// 若缓存可用且存在有效缓存，则创建缓存请求任务，加载缓存中的数据
			if (mRequestDataCache.isEffective()) {
				mRequestTask = new CacheRequestTask(mRequestDataCache,
						mCacheOnRequestTaskListener);
				// 若缓存可提供预加载，则创建预加载请求任务，加载缓存中的数据
			} else if (mNeedPreload && mRequestDataCache.isExist()) {
				mRequestTask = new CacheRequestTask(mRequestDataCache,
						mPreloadOnRequestTaskListener);
			}
		}

		if (mRequestTask == null) {
			mRequestTask = getWebRequestTask(mWebOnRequestTaskListener);
		}

		mRequestTask.run();
	}

	/**
	 * 获取执行网络请求的任务，可由子类进行重写
	 * 
	 * @return
	 */
	protected WebRequestTask getWebRequestTask(
			OnRequestTaskListener webRequestTaskListener) {
		WebRequestTask webRequestTask = new WebRequestTask(mContext,
				getAdjustRequestUrl(), getRequestHeaders(), getCombineParams(),
				getPriority(), webRequestTaskListener);
		webRequestTask.setRequestType(getRequestType());
		return webRequestTask;
	}

	/**
	 * 取消请求
	 */
	public void cancel() {
		super.cancel();
		if (mRequestTask != null) {
			mRequestTask.cancel();
		}
	}

	/**
	 * 开始发起数据请求
	 * 
	 */
	public void request() {
		if (DEBUG) {
			Log.d(TAG, "make a request");
		}
		executeAsyncTask();
	}

	/**
	 * Reload 清除当前缓存，重新加载数据
	 */
	public void reload() {
		if (DEBUG) {
			Log.d(TAG, "prepare for reload");
		}
		if (canUseCache()) {
			mRequestDataCache.delete();
		}
		request();
	}

	/**
	 * 处理返回的json/xml数据
	 * 
	 * @param data
	 * @param isFromPreload
	 */
	private synchronized void parseResult(String data, boolean isFromPreload) {
		AbstractIFItem item = null;
		/*
		 * 如果可以进行自动解析
		 */
		if (canUseAutoParse()) {
			item = AbstractIFItem.getInstance(mAutoParseClass);
			/*
			 * 如果成功自动解析
			 */
			if (item != null && item.parseData(data)) {
				if (isFromPreload) {
					handlePreloadResult(item);
				} else {
					handleResult(item);
				}
				return;
			}
		}

		/*
		 * 需要子类手动解析
		 */
		item = handleUnparseResult(data);
		if (isFromPreload) {
			handlePreloadResult(item);
		} else {
			handleResult(item);
		}
	}

	/**
	 * 获取修正后的请求url地址，去除重复参数、添加http头协议
	 * 
	 * @return
	 */
	protected final String getAdjustRequestUrl() {
		// 修正非http协议的url
		String url = filterParams(getRequestUrl(), getCombineParams());
		if (url.length() < HTTP_URL_PREFIX_LENGTH
				|| !HTTP_URL_PREFIX.equalsIgnoreCase(url.substring(0,
						HTTP_URL_PREFIX_LENGTH))) {
			url = HTTP_URL_PREFIX_LENGTH + url;
		}
		return url;
	}

	/**
	 * 整合请求参数
	 * 
	 * @return
	 */
	protected final List<NameValuePair> getCombineParams() {
		List<NameValuePair> request = getRequestParams();
		List<NameValuePair> extra = getExtraParams();
		List<NameValuePair> combine = new LinkedList<NameValuePair>();
		if (request != null) {
			combine.addAll(request);
		}
		if (extra != null) {
			combine.addAll(extra);
		}
		return combine;
	}

	/**
	 * 过滤参数，将Url中那些在Param中出现的参数过滤掉
	 * 
	 * @param orginalUrl
	 *            原始请求Url
	 * @param params
	 *            请求参数
	 * @return 过滤后的Url
	 */
	private String filterParams(String orginalUrl, List<NameValuePair> params) {

		String url = new String(orginalUrl);

		if (params != null) {
			for (NameValuePair param : params) {
				Pattern pattern = Pattern.compile("[\\?\\&]" + param.getName()
						+ "\\=[^\\&\\?]*");
				Matcher matcher = pattern.matcher(url);
				if (matcher.find()) {
					String group = matcher.group();
					if (group.startsWith("?")) {
						url = matcher.replaceAll("?");
					} else {
						url = matcher.replaceAll("");
					}
				}
			}
		}

		return url;
	}

	/**
	 * @return the requestType
	 */
	protected RequestType getRequestType() {
		return mRequestType;
	}

	/**
	 * @param requestType
	 *            the requestType to set
	 */
	protected void setRequestType(RequestType requestType) {
		this.mRequestType = requestType;
	}

	/**
	 * 请求头信息
	 * 
	 * @return Headers
	 */
	protected abstract List<NameValuePair> getRequestHeaders();

	/**
	 * 请求参数
	 * 
	 * @return Parameters
	 */
	protected abstract List<NameValuePair> getRequestParams();

	/**
	 * 添加额外的请求参数，例如可在父类接口中添加鉴权认证参数等不涉及具体业务逻辑的参数。
	 * 
	 * @return
	 */
	protected abstract List<NameValuePair> getExtraParams();

	/**
	 * 请求地址
	 * 
	 * @return Url
	 */
	protected abstract String getRequestUrl();

	/**
	 * 处理请求结果
	 * 
	 * @param item
	 */
	protected abstract void handleResult(AbstractIFItem item);

	/**
	 * 处理预加载的请求结果
	 * 
	 * @param item
	 */
	protected void handlePreloadResult(AbstractIFItem item) {
		/*
		 * 子类如需preload必须重写，不作为abstract方法
		 */
		if (mNeedPreload) {
			throw new NeedOverrideException(
					"if need a preload process , you hava to override the handlePreloadResult method");
		}
	}

	/**
	 * 手动解析获取到的JSON/XML数据
	 * 
	 * @param result
	 *            result
	 */
	protected AbstractIFItem handleUnparseResult(String result) {
		/*
		 * 子类可重写，但不作为abstract方法
		 */
		return null;
	}

	/**
	 * 是否可以使用缓存
	 * 
	 * @return 是否可以使用缓存
	 */
	private boolean canUseCache() {
		return mRequestDataCache != null;
	}

	/**
	 * 在请求之前填充cacheParam之中的filetag，供requestor查找cache文件。默认为将Url及参数拼接而成的字符串，
	 * 子类中可自行重写实现所需的缓存策略。
	 */
	private void fillCacheFileTag() {
		StringBuffer sb = new StringBuffer();
		sb.append(getRequestUrl());

		List<NameValuePair> params = getRequestParams();
		if (params != null) {
			for (NameValuePair nameValuePair : params) {
				sb.append(nameValuePair.getName());
				sb.append(nameValuePair.getValue());
			}
		}

		if (canUseCache()) {
			mRequestDataCache.getDataCacheParams().cacheFileTag = sb.toString();
		}
	}

	/**
	 * 如果需要,缓存数据
	 * 
	 * @param data
	 *            数据
	 */
	private void cacheDataIfNeed(String data) {
		if (canUseCache()
				&& mRequestDataCache.getDataCacheParams().cacheEnabled) {
			if (DEBUG) {
				Log.d(TAG, "request cache has saved");
			}

			mRequestDataCache.save(data);
		}
	}

	/**
	 * 是否存在可提供预加载的缓存，方便页面进行显示控制，以适配不同的策略
	 * 
	 * @return
	 */
	public boolean hasPreloadCache() {
		fillCacheFileTag();
		if (canUseCache() && mRequestDataCache.isExist()) {
			return true;
		}
		return false;
	}

	/**
	 * 对本次request，打开cache预加载，本次request会同时从cache和网络读取数据，另外，本次网络数据会缓存
	 * 用于对网络数据未能完成请求之前对用户的数据呈现。
	 * 
	 * @param cacheLoadListener
	 *            回调
	 */
	public void turnOnPreloadFromCache(
			OnPreloadFromCacheListener cacheLoadListener) {
		mNeedPreload = true;
		mIsPreloaded = false;
		mOnPreloadFromCacheListener = cacheLoadListener;
	}

	/**
	 * 关闭cache预加载,默认为关闭状态
	 */
	public void turnOffPreloadFromCache() {
		mNeedPreload = false;
		mOnPreloadFromCacheListener = null;
	}

	/**
	 * 是否已经完成了预加载
	 * 
	 * @return
	 */
	public boolean isPreloaded() {
		return mIsPreloaded;
	}

	/**
	 * 设置自动解析类型
	 * 
	 * @param autoParseClass
	 */
	public void setAutoParseClass(Class<? extends AbstractIFItem> autoParseClass) {
		this.mAutoParseClass = autoParseClass;
	}

	/**
	 * 是否可以进行自动解析
	 * 
	 * @return
	 */
	private boolean canUseAutoParse() {
		return mAutoParseClass != null;
	}

	/**
	 * 获取cache回调
	 * 
	 * @author xuwei
	 */
	public interface OnPreloadFromCacheListener {

		/**
		 * cache获取成功
		 * 
		 * @param requestor
		 *            requestor
		 */
		void onCacheLoaded(AbstractRequestor requestor);
	}

}
