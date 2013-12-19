package com.ifeng;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.ifeng.android.R;
import com.ifeng.util.SdkVersionUtils;
import com.ifeng.util.Utility;
import com.ifeng.util.imagecache.ImageCache.ImageCacheParams;
import com.ifeng.util.imagecache.ImageFetcher;
import com.ifeng.util.logging.Log;
import com.ifeng.util.model.AbstractModel;
import com.ifeng.util.model.AbstractModel.OnModelProcessListener;
import com.ifeng.util.model.ModelManageQueue;

/**
 * BaseFragment所有实现的Fragment页面应将继承自此类
 * 
 * @author Xuwei
 * 
 */
public abstract class BaseFragment extends Fragment implements
		OnModelProcessListener {

	/** tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug开关 */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;
	/** 默认image cache dir */
	private final String IMAGE_CACHE_DIR = "images";

	/** 图片缓存文件夹 */
	protected String mImageCacheDir = "images";
	/** 图片最大尺寸 */
	protected int mImageSize;
	/** 图片加载工具类 */
	protected ImageFetcher mImageFetcher;

	/** model管理类 */
	protected ModelManageQueue mModelManageQueue;

	/** 像素密度 */
	protected float mDensity;
	/** 字体sp计算所需像素密度 */
	protected float mScaleDensity;
	/** 宽度 dip */
	protected int mWidth;
	/** 高度 dip */
	protected int mHeight;

	/** 是否需要初始化ImageFetcher */
	private boolean mIsNeedImageFetcher = true;

	/** 跳转至下一页面的离开动画 */
	private int mPushLeftAnim = R.anim.activity_anim_push_left_out;
	/** 跳转至下一页面的进入动画 */
	private int mPushInAnim = R.anim.activity_anim_push_left_in;

	/**
	 * 需要一个空的构造方法
	 */
	public BaseFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onInit();
	}

	/**
	 * 设置是否需要imageFetcher图片加载框架，ImageFetcher会涉及使用到
	 * {@link android.app.Fragment#setRetainInstance(boolean)}方法，该方法并不适用于Nested
	 * Fragment。
	 * 
	 * @param isNeed
	 */
	protected void setNeedImageFetcher(boolean isNeed) {
		mIsNeedImageFetcher = isNeed;
	}

	/**
	 * 完成相应的初始化操作
	 */
	protected void onInit() {
		mDensity = Utility.getDensity(getActivity());
		mScaleDensity = Utility.getScaledDensity(getActivity());
		mHeight = (int) (Utility.getScreenHeight(getActivity()) / mDensity);
		mWidth = (int) (Utility.getScreenWidth(getActivity()) / mDensity);

		if (mIsNeedImageFetcher) {
			setupImageFetcher();
		}

		mModelManageQueue = new ModelManageQueue();
		setupModel();
	}

	/**
	 * 设定独立的缓存配置，如：在列表缩略图或大图展示之中，图片尺寸及缓存目录都需要进行分别的设置。
	 */
	protected void setImageCacheParams() {
		if (mIsNeedImageFetcher) {
			if (DEBUG) {
				Log.w(TAG, "should override the setImageCacheParams method");
			}
		}
		// do nothing
	}

	/**
	 * 初始化ImageFetcher
	 */
	private void setupImageFetcher() {
		setImageCacheParams();
		if (mImageSize == 0) {
			mImageSize = (mHeight > mWidth ? mHeight : mWidth) / 2;
		}

		if (mImageCacheDir == null) {
			mImageCacheDir = IMAGE_CACHE_DIR;
		}

		ImageCacheParams params = new ImageCacheParams(getActivity(),
				mImageCacheDir);
		params.setMemCacheSizePercent(0.3f);

		mImageFetcher = new ImageFetcher(getActivity(), mImageSize,
				mImageCacheDir);
		mImageFetcher.addImageCache(getFragmentManager(), params);
	}

	/**
	 * 加载当前页面需要用到的model
	 */
	protected void setupModel() {
		// do nothing
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mImageFetcher != null) {
			mImageFetcher.setPauseWork(false);
		}

		mModelManageQueue.pauseQueue(false);
	}

	@Override
	public void onPause() {
		super.onPause();

		if (mImageFetcher != null) {
			mImageFetcher.setPauseWork(true);
		}

		mModelManageQueue.pauseQueue(true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mImageFetcher != null) {
			mImageFetcher.setExitTasksEarly(true);
			// mImageFetcher.clearMemoryCache();
			mImageFetcher.flushCache();
			mImageFetcher.closeCache();
		}

		mModelManageQueue.clearQueue();
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		if (SdkVersionUtils.hasFroyo()) {
			getActivity().overridePendingTransition(mPushInAnim, mPushLeftAnim);
		}
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

}