package com.ifeng.util.net.requestor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import com.ifeng.BaseApplicaion;
import com.ifeng.util.SdkVersionUtils;
import com.ifeng.util.logging.Log;

/**
 * cache 管理工具类
 * 
 * @author xuwei
 */
public class RequestDataCache {

	/** log tag. */
	private static final String TAG = RequestDataCache.class.getSimpleName();

	/** if enabled, logcat will output the log. */
	protected static final boolean DEBUG = true & BaseApplicaion.DEBUG;

	/** 默认硬盘缓存区大小 */
	private static final int DEFAULT_CACHE_SIZE = 1024 * 1024 * 2; // 2MB
	/** 默认开启缓存 */
	private static final boolean DEFAULT_CACHE_ENABLED = true;
	/** 默认缓存有效期 */
	private static final int DEFAULT_CACHE_VALIDITY_TIME = 5 * 60 * 1000; // 5mins
	/** 默认缓存作废期 */
	private static final int DEFAULT_CACHE_EXIST_TIME = 24 * 60 * 60 * 1000; // 24h
	/** 默认缓存文件标识 */
	private static final String DEFAULT_CACHE_TAG = "Cache";

	/** 配置参数文件名 */
	private static final String FILENAME_PROPERTY = ".property";

	/** 配置参数_缓存大小 */
	private static final String KEY_CACHE_SIZE = "size";
	/** 配置参数_缓存有效期 */
	private static final String KEY_CACHE_VALIDITY_TIME = "validityTime";
	/** 配置参数_缓存废弃期 */
	private static final String KEY_CACHE_EXIST_TIME = "existTime";

	/** context */
	private Context mContext;
	/** cache模块唯一id */
	private String mId;
	/** cache文件目录 */
	private File mCacheFileDir;
	/** cache配置参数 */
	private DataCacheParams mDataCacheParams;

	/**
	 * 构造
	 * 
	 * @param context
	 *            context
	 * @param id
	 *            缓存模块唯一标识
	 */
	public RequestDataCache(Context context, String id) {
		this(context, id, new DataCacheParams());
	}

	/**
	 * 构造
	 * 
	 * @param context
	 *            context
	 * @param id
	 *            缓存模块唯一标识
	 * @param dataCacheParams
	 *            缓存参数配置
	 */
	public RequestDataCache(Context context, String id,
			DataCacheParams dataCacheParams) {
		mContext = context.getApplicationContext();
		mId = id;
		mDataCacheParams = dataCacheParams;
		init();
	}

	/**
	 * 初始化缓存目录及配置文件
	 */
	private void init() {
		// 根据当前Sd卡状态，创建适宜的缓存空间目录
		final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState()) ? getExternalCacheDir(mContext)
				.getPath() : mContext.getCacheDir().getPath();
		mCacheFileDir = new File(cachePath + File.separator
				+ hashKeyForDisk(mId));
		if (DEBUG) {
			Log.d(TAG, "makeCacheDir =" + mCacheFileDir.getAbsolutePath());
		}
		mCacheFileDir.mkdirs();

		// 检测property文件状态
		File propertyFile = new File(mCacheFileDir.getAbsolutePath()
				+ File.separator + FILENAME_PROPERTY);
		Properties properties = new Properties();
		if (!propertyFile.exists()) {
			try {
				propertyFile.createNewFile();
			} catch (IOException e) {
				if (DEBUG) {
					Log.e(TAG, e);
				}
				return;
			}
		}

		// 更新property配置文件
		try {
			// 不建议使用put方法，properties继承自hashtable，若使用put方法加入非String类型参数则会出现castexception
			properties.setProperty(KEY_CACHE_SIZE, mDataCacheParams.cacheSize
					+ "");
			properties.setProperty(KEY_CACHE_VALIDITY_TIME,
					mDataCacheParams.cacheValidityTime + "");
			properties.setProperty(KEY_CACHE_EXIST_TIME,
					mDataCacheParams.cacheExistTime + "");
			properties.store(new FileOutputStream(propertyFile, false), null);
		} catch (FileNotFoundException e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		} catch (IOException e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		}

		cleanWasteCache();
		cleanOverrangeCache();
	}

	/**
	 * 清理已经作废的缓存文件
	 */
	private void cleanWasteCache() {
		if (mCacheFileDir != null && mCacheFileDir.exists()) {
			// 加载缓存文件列表
			File[] cacheFiles = mCacheFileDir.listFiles();
			if (cacheFiles != null) {
				for (File file : cacheFiles) {
					// 删除已经到达作废期的缓存文件
					if (System.currentTimeMillis() - file.lastModified() > mDataCacheParams.cacheExistTime) {
						file.delete();
					}
				}
			}
		}
	}

	/**
	 * 若超出了缓存空间限制，则清理掉超过有效期的文件
	 */
	private void cleanOverrangeCache() {
		if (mCacheFileDir != null) {
			long currentSize = 0;
			File[] cacheFiles = mCacheFileDir.listFiles();
			if (cacheFiles != null) {
				for (File file : cacheFiles) {
					currentSize += file.length();
				}
			}
			if (currentSize < mDataCacheParams.cacheSize) {
				// 若未超出限制则不继续执行
				return;
			}
		}
		if (mCacheFileDir != null && mCacheFileDir.exists()) {
			// 加载缓存文件列表
			File[] cacheFiles = mCacheFileDir.listFiles();
			if (cacheFiles != null) {
				for (File file : cacheFiles) {
					// 删除已经失效的缓存文件
					if (System.currentTimeMillis() - file.lastModified() > mDataCacheParams.cacheValidityTime) {
						file.delete();
					}
				}
			}
		}
	}

	/**
	 * 保存
	 * 
	 * @param data
	 *            data
	 * @return 成功
	 */
	public boolean save(String data) {
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(getCurrentTagCache());
			fileWriter.write(data);
			fileWriter.close();
		} catch (IOException ex) {
			if (DEBUG) {
				Log.e(TAG, ex);
			}
			return false;
		} finally {
			try {
				if (fileWriter != null) {
					fileWriter.close();
				}
			} catch (IOException ex) {
				if (DEBUG) {
					Log.e(TAG, ex);
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否存在有效缓存
	 * 
	 * @return 是否存在有效缓存
	 */
	public boolean isEffective() {
		File cacheFile = getCurrentTagCache();
		if (cacheFile != null
				&& cacheFile.exists()
				&& System.currentTimeMillis() - cacheFile.lastModified() < mDataCacheParams.cacheValidityTime) {
			// 若存在缓存文件，且尚未超过有效期
			return true;
		}
		return false;
	}

	/**
	 * 是否存在缓存可供预加载
	 * 
	 * @return 是否存在缓存可供预加载
	 */
	public boolean isExist() {
		File cacheFile = getCurrentTagCache();
		if (cacheFile != null
				&& cacheFile.exists()
				&& System.currentTimeMillis() - cacheFile.lastModified() < mDataCacheParams.cacheExistTime) {
			// 若存在缓存文件，且尚未超过作废期
			return true;
		}
		return false;
	}

	/**
	 * 加载
	 * 
	 * @return 数据
	 */
	public String load() {
		BufferedReader br = null;
		String ret = null;
		try {
			br = new BufferedReader(new FileReader(getCurrentTagCache()));
			String line = null;
			StringBuffer sb = new StringBuffer((int) getCurrentTagCache()
					.length());
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			ret = sb.toString();
		} catch (IOException e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					if (DEBUG) {
						Log.e(TAG, e);
					}
				}
			}
		}

		return ret;
	}

	/**
	 * 删除
	 * 
	 * @return 是否删除成功
	 */
	public boolean delete() {
		File cacheFile = getCurrentTagCache();
		if (cacheFile != null) {
			return cacheFile.delete();
		}
		return true;
	}

	private File getCurrentTagCache() {
		File cacheFile = new File(mCacheFileDir.getAbsolutePath()
				+ File.separator
				+ hashKeyForDisk(mDataCacheParams.cacheFileTag));
		return cacheFile;
	}

	/**
	 * 获取缓存机制的配置
	 * 
	 * @return 缓存策略配置
	 */
	public DataCacheParams getDataCacheParams() {
		return mDataCacheParams;
	}

	/**
	 * Get the external app cache directory.
	 * 
	 * @param context
	 *            The context to use
	 * @return The external cache dir
	 */
	@SuppressLint("NewApi")
	public
	static File getExternalCacheDir(Context context) {
		File cacheDir = null;
		if (SdkVersionUtils.hasFroyo()) {
			cacheDir = context.getExternalCacheDir();
		}

		// Before Froyo we need to construct the external cache dir ourselves
		if (cacheDir == null) {
			cacheDir = new File(Environment.getExternalStorageDirectory()
					.getPath() + "/ifeng/data/cache/");
		}
		return cacheDir;
	}

	/**
	 * A hashing method that changes a string (like a URL) into a hash suitable
	 * for using as a disk filename.
	 */
	private static String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	private static String bytesToHexString(byte[] bytes) {
		// http://stackoverflow.com/questions/332079
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	/**
	 * 配置缓存策略的参数类，在变更请求的时候需要重新设置{@link #cacheFileTag}
	 * 
	 * @author xuwei
	 * 
	 */
	public static class DataCacheParams {
		/** 是否提供缓存功能 */
		public boolean cacheEnabled = DEFAULT_CACHE_ENABLED;
		/** 当前请求的缓存文件标识 */
		public String cacheFileTag = DEFAULT_CACHE_TAG;
		/** 缓存区域大小 */
		public int cacheSize = DEFAULT_CACHE_SIZE;
		/** 缓存有效期 */
		public int cacheValidityTime = DEFAULT_CACHE_VALIDITY_TIME;
		/** 缓存生存期 */
		public int cacheExistTime = DEFAULT_CACHE_EXIST_TIME;
	}
}
