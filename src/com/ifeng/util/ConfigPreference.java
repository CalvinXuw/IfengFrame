package com.ifeng.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.ifeng.BaseApplicaion;
import com.ifeng.util.logging.Log;

/**
 * 客户端相关配置文件
 * 
 * @author Calvin.Xuw
 * 
 */
public abstract class ConfigPreference {

	/** tag */
	protected final String TAG = getClass().getSimpleName();
	/** debug switch */
	protected final boolean DEBUG = BaseApplicaion.DEBUG;

	/** sp filename */
	private final static String SP_NAME = ".preference";
	/** sp access mode */
	private final static int MODE = Context.MODE_PRIVATE;

	/** sp filename */
	private String mSPFileName;
	/** sp access mode */
	private int mSPAccessMode;

	/** context */
	protected Context mContext;
	/** SharedPreferences */
	private SharedPreferences mSharedPreferences;

	/**
	 * constructor
	 * 
	 * @param context
	 */
	public ConfigPreference(Context context) {
		this(context, SP_NAME, MODE);
	}

	/**
	 * constructor
	 * 
	 * @param context
	 */
	public ConfigPreference(Context context, String spName, int mode) {
		if (TextUtils.isEmpty(spName)) {
			throw new IllegalArgumentException(
					"config filename cannot be empty");
		}
		mContext = context.getApplicationContext();
		String packageName = context.getPackageName();
		mSPFileName = packageName + spName;
		mSPAccessMode = mode;
		mSharedPreferences = context.getSharedPreferences(mSPFileName,
				mSPAccessMode);
	}

	/**
	 * whether the preferences contains a preference.
	 * 
	 * @param key
	 * @return
	 */
	protected boolean contain(String key) {
		return mSharedPreferences.contains(key);
	}

	/**
	 * get string value for key
	 * 
	 * @param key
	 * @return
	 */
	protected String getString(String key) {
		try {
			return mSharedPreferences.getString(key, null);
		} catch (ClassCastException e) {
			if (DEBUG) {
				Log.e(TAG, "preference with this name that is not a String.");
				Log.e(TAG, e);
			}
			return null;
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "caught unknows exception!");
				Log.e(TAG, e);
			}
			return null;
		}
	}

	/**
	 * get boolean value for key
	 * 
	 * @param key
	 * @return
	 */
	protected boolean getBoolean(String key) {
		try {
			return mSharedPreferences.getBoolean(key, false);
		} catch (ClassCastException e) {
			if (DEBUG) {
				Log.e(TAG, "preference with this name that is not a boolean.");
				Log.e(TAG, e);
			}
			return false;
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "caught unknows exception!");
				Log.e(TAG, e);
			}
			return false;
		}
	}

	/**
	 * get int value for key
	 * 
	 * @param key
	 * @return
	 */
	protected int getInt(String key) {
		try {
			return mSharedPreferences.getInt(key, 0);
		} catch (ClassCastException e) {
			if (DEBUG) {
				Log.e(TAG, "preference with this name that is not a int.");
				Log.e(TAG, e);
			}
			return 0;
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "caught unknows exception!");
				Log.e(TAG, e);
			}
			return 0;
		}
	}

	/**
	 * get long value for key
	 * 
	 * @param key
	 * @return
	 */
	protected long getLong(String key) {
		try {
			return mSharedPreferences.getLong(key, 0);
		} catch (ClassCastException e) {
			if (DEBUG) {
				Log.e(TAG, "preference with this name that is not a long.");
				Log.e(TAG, e);
			}
			return 0;
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "caught unknows exception!");
				Log.e(TAG, e);
			}
			return 0;
		}
	}

	/**
	 * set string value for key
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	protected boolean putString(String key, String value) {
		Editor editor = mSharedPreferences.edit();
		editor.putString(key, value);
		return editor.commit();
	}

	/**
	 * set boolean value for key
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	protected boolean putBoolean(String key, boolean value) {
		Editor editor = mSharedPreferences.edit();
		editor.putBoolean(key, value);
		return editor.commit();
	}

	/**
	 * set int value for key
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	protected boolean putInt(String key, int value) {
		Editor editor = mSharedPreferences.edit();
		editor.putInt(key, value);
		return editor.commit();
	}

	/**
	 * set long value for key
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	protected boolean putLong(String key, long value) {
		Editor editor = mSharedPreferences.edit();
		editor.putLong(key, value);
		return editor.commit();
	}
}
